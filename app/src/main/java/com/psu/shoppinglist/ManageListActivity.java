package com.psu.shoppinglist;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import ai.api.model.Result;
import ai.api.AIConfiguration;
import ai.api.AIDataService;
import ai.api.model.AIRequest;
import ai.api.AIServiceException;
import ai.api.AIListener;
import ai.api.AIService;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import ai.api.model.Result;
import com.google.gson.JsonElement;


public class ManageListActivity extends AppCompatActivity {
    boolean muteVoice;
    String listName;
    ShoppingList sl;
    final int MAX_NUM_ITEMS = 100; //allow a max of 100 items on the list!
    private static final int SPEECH_REQUEST_CODE = 0;
    TextToSpeech t1;
    int numItem;
    Boolean noToast;

    String menuHelp = "This is the Manage List Activity page. \n\nYou can Add, Remove, Count items, Or go back to the Main Menu. " +
            "Use the following commands: " +
            "\n\n\t- Add apples." +
            "\n\t- Remove apples." +
            "\n\t- Count items." +
            "\n\t- Back to Main Menu." +
            "\n\t- Exit." +
            "\n\t- Who created this app?" +
            "\n\nThese functions are also accessible from the overflow menu.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_list);
        //muteVoice = getIntent().getExtras().getBoolean("muteVice", false);
        listName = getIntent().getExtras().getString("listName", "Error");
        sl = new ShoppingList(listName, null);


        setTitle(listName);
        initializeSpeech(listName);

        // reveal listname content
        listItems();
    }

    /**
     * Receiving speech input
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            makeAPIRequest(spokenText);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    protected void makeAPIRequest(String speech) {

        Log.v ("----API req:", speech + ".");
        final AIRequest aiRequest = new AIRequest();
        final AIConfiguration config = new AIConfiguration("903fe05ba6064111aed341dc9c051e59",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        final AIDataService aiDataService = new AIDataService(this, config);
        aiRequest.setQuery(speech);

        new AsyncTask<AIRequest, Void, AIResponse>() {
            @Override
            protected AIResponse doInBackground(AIRequest... requests) {
                final AIRequest request = requests[0];
                try {
                    final AIResponse response = aiDataService.request(aiRequest);
                    return response;
                } catch (AIServiceException e) {
                }
                return null;
            }
            @Override
            protected void onPostExecute(AIResponse aiResponse) {
                if (aiResponse != null) {
                    Result result = aiResponse.getResult();

                    // Get parameters
                /*    String parameterString = "";
                    if (result.getParameters() != null && !result.getParameters().isEmpty()) {
                        for (final Map.Entry<String, JsonElement> entry : result.getParameters().entrySet()) {
                            parameterString += "(" + entry.getKey() + ", " + entry.getValue() + ") ";
                        }
                    }*/

                    // Need to decide what to do with our response:
                    String intent = result.getMetadata().getIntentName();
                    String response = "";
                    Log.v ("-----------API: ", intent + ".");

                    if (intent == null) {
                        // making use of API.IA's cleverness!
                        response = result.getFulfillment().getSpeech();
                        if (response.isEmpty())
                            response = "The A.I. was not able to determine the intent. Please try a different command.";
                    } else {
                        noToast = false;
                        switch (intent) {
                            case "addItem":
                                response = vAddItem(result);
                                break;
                            case "removeItem":
                                response = vRemoveItem(result);
                                listItems();
                                break;
                            case "count":
                                response = vCountItem(result);
                                break;
                            case "goBackMain":
                                finish();
                                break;

                            case "about":
                                getAbout();
                                // Need to make sure that the text to speech pronounces names correctly.
                                response = "This app is developed by: Danny Madden and Gustaf Hegnell.";
                                noToast = true;
                                break;
                            case "exit":
                                doExit();
                                break;

                            case "help":
                                getHelp();
                                response = "Managing list activity. You can Add, Remove, Count items, Or go back to the Main Menu.\n" +
                                        "Supported commands are:\n " +
                                        "- Add apples.\n" +
                                        "- Remove apples.\n" +
                                        "- Count items.\n" +
                                        "- Back to Main Menu.\n" +
                                        "- Exit.\n Or \n" +
                                        "- Who created this app? \n";
                                noToast = true;
                                break;

                            default:
                                response = "I heard: " + result.getResolvedQuery() + ". I don't know what to do with that.";
                        }
                    }
                    if (noToast == false) {
                        for (int p = 0; p < 2; p++) {
                            Toast.makeText(getApplicationContext(), response, Toast.LENGTH_LONG).show();
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        t1.speak(response, TextToSpeech.QUEUE_FLUSH, null, null);
                    } else
                        t1.speak(response, TextToSpeech.QUEUE_FLUSH, null);

                }
            }
        }.execute(aiRequest);
    }

    // we can only add item to the listName that was given when we start
    // this activity. Adding item to another list will fail.
    public String vAddItem (Result result) {
        String respond = "";
        String itemName = result.getStringParameter("itemName");
        String AIlistName = result.getStringParameter("listName", "as-def");

        // verify listName and AIlistName
        // "add apple" would return AIlistName = "as-def".
        // if we see "as-def" then we assume user wants to add it to the current list.
        if (!AIlistName.contentEquals(listName) && !AIlistName.contentEquals("as-def")) {
            respond = "I am not that advanced. " + AIlistName + " is not opened. Go back to the main activity to open it first.";
        }else {
            // check itemName
            if (!itemName.isEmpty()) {
                String inputString = itemName.trim();
                if (inputString != null && !inputString.isEmpty()) {
                    // Capitalize the first letter
                    String theItem = inputString.trim().substring(0, 1).toUpperCase() +
                            inputString.trim().substring(1) + "\n";

                    if (!itemIsInList(theItem)) {
                        try {
                            FileOutputStream fos = openFileOutput(sl.listname + ".txt", Context.MODE_APPEND);
                            fos.write(theItem.getBytes());
                            fos.close();
                            listItems();
                            respond = "Ok. " + itemName + " has been added to " + listName + " list.";
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        respond = itemName + " is already on your " + listName + " list.";
                    }
                }
            } else {
                respond = "Add item to "+ listName + " failed. A.I. did not return an item to me because the item is likely not in A.I's domain.";
            }
        }

        return respond;
    }

    public String vRemoveItem (Result result) {
        String respond = "";
        String AIlistName = result.getStringParameter("listName", "as-def");
        String itemName = result.getStringParameter("itemName");

        if (!AIlistName.contentEquals(listName) && !AIlistName.contentEquals("as-def")) {
            respond = "I am not that advanced. " + AIlistName + " is not opened. Go back to the main activity to open it first.";
        } else {
            // check item
            if (!itemName.isEmpty()) {
                // Capitalize the first letter
                String theItem = itemName.trim().substring(0, 1).toUpperCase() +
                        itemName.trim().substring(1) + "\n";
                theItem = theItem.trim();

                // check if it's in the list
                if (itemIsInList(theItem)) {
                    // open file
                    try {
                        InputStream is = openFileInput(sl.listname + ".txt");
                        File newFile = new File("toberenamed" + ".txt");
                        String line;
                        FileOutputStream fos = openFileOutput(newFile.getName(), Context.MODE_APPEND);
                        if (is != null) {
                            InputStreamReader inputReader = new InputStreamReader(is);
                            BufferedReader buffReader = new BufferedReader(inputReader);
                            line = buffReader.readLine();
                            while (line != null) {
                                // Write all unmatched item to a new file.
                                if (!line.isEmpty() && !line.contentEquals(theItem)) {
                                    fos.write(line.getBytes());
                                    fos.write("\n".getBytes());
                                    Log.v("vRemoveItem----", "unmatched " + line + "." + theItem + ".");

                                }
                                line = buffReader.readLine();
                            }
                        }
                        fos.flush();
                        fos.close();
                        is.close();

                        // must have the actual file object for rename to work!
                        File oldFile = myGetFile(sl.listname);
                        File special = myGetFile("toberenamed");
                        if (oldFile != null) {
                            oldFile.delete();
                            if (special != null && special.exists()) {
                                Boolean status = special.renameTo(oldFile);
                                Log.v("RemoveAnItem:", "rname file status: " + status);
                            } else
                                Log.v("RemoveAnItem:", "rename special doesn't exist?!");
                        }
                        respond = "OK. " + itemName + " is removed from " + listName + " list.";

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    listItems();
                }
                else {
                    respond = "Failed to remove. " + theItem + " is not on the list.";
                }
            }
            else {
                respond = "Failed to remove. A.I. didn't return an item name because likely item isn't in the A.I.'s domain";
            }
        }

        return respond;
    }

    public String vCountItem (Result result) {
        String respond = "";
        if (numItem == 0)
            respond = "You do not have any item on the " + listName + " list";
        else
            respond = "You have " + numItem +" items on the " + listName + " list.";
        return respond;
    }

    public String countItem() {
        String respond;
        if (numItem == 0)
            respond = "You do not have any item on the " + listName + " list";
        else
            respond = "You have " + numItem +" items on the " + listName + " list.";
        return respond;
    }

    // Read in the file list
    public void listItems() {
        List<String> ITEMLIST= new ArrayList<String>(MAX_NUM_ITEMS);
        String line;
        numItem = 0;
        try {
            InputStream is = openFileInput(sl.listname + ".txt");
            if (is != null) {
                InputStreamReader inputReader = new InputStreamReader(is);
                BufferedReader buffReader = new BufferedReader(inputReader);
                do {
                    line = buffReader.readLine();
                    if (line != null && !line.isEmpty()) {
                        ITEMLIST.add(line);
                        numItem++;
                        //Log.v ("--listItems", "line has:" + line + ".");
                    }
                } while (line != null);

            }
            is.close();
        } catch (IOException ioe) {
                ioe.printStackTrace();
        }
        sl.items = ITEMLIST;
        if (ITEMLIST.isEmpty()) {
            ITEMLIST.add("no item");
        }

        Collections.sort(ITEMLIST);

        ArrayAdapter adapter = new ArrayAdapter<String>(this,R.layout.content_manage_list,ITEMLIST);
        ListView listView = (ListView) findViewById(R.id.list_item);
        listView.setAdapter(adapter);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                String selected = ((TextView)view).getText().toString();
                if (!selected.contentEquals("no item"))
                    removeAnItem(selected);
                else
                    Toast.makeText(getApplicationContext(), "List is empty. Nothing to do.", Toast.LENGTH_LONG).show();

                return true;
            }
        });
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    public void initializeSpeech (String name) {
        final String welcome = "Ok, managing " + name + " list.";

        t1 = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = t1.setLanguage(Locale.US);

                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    }
                    Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_SHORT).show();

                    if (!muteVoice) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            t1.speak(welcome, TextToSpeech.QUEUE_FLUSH, null, null);
                        } else
                            t1.speak(welcome.toString(), TextToSpeech.QUEUE_FLUSH, null);
                    }
                } else
                    Log.v("DEBUG", "TTS failed to start");
            }
        });
    }

    // add an to list
    public void addAnItem() {
        final AlertDialog.Builder addDialog = new AlertDialog.Builder(this);
        addDialog.setTitle("Add an item to " + sl.listname);
        final EditText input = new EditText(this);
        input.setSingleLine();
        addDialog.setView(input);

        addDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String inputString = input.getText().toString();

                if (inputString != null && !inputString.isEmpty()) {
                    // Capitalize the first letter
                    String theItem = inputString.trim().substring(0,1).toUpperCase() +
                        inputString.trim().substring(1)+"\n";

                    if (!itemIsInList(theItem)) {
                        try {
                            FileOutputStream fos = openFileOutput(sl.listname + ".txt", Context.MODE_APPEND);
                            fos.write(theItem.getBytes());
                            fos.close();
                            listItems();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    else
                        Toast.makeText(getApplicationContext(), theItem.replace("\n","") + " is already on the list!", Toast.LENGTH_LONG).show();
                }
            }
        });

        addDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        addDialog.create();
        addDialog.show();
    }

    public boolean itemIsInList (String s) {
        try {
            InputStream is = openFileInput(sl.listname + ".txt");
            String line;

            if (is != null) {
                InputStreamReader inputReader = new InputStreamReader(is);
                BufferedReader buffReader = new BufferedReader(inputReader);

                line = buffReader.readLine();
                while (line != null ){

                    if (line.contentEquals(s.replace("\n", ""))) {
                        Log.v ("itemIsInList:", "found " + s.replace("\n", ""));
                        is.close();
                        return true;
                    }
                    else
                        Log.v ("itemIsInList:", line + " not equal: " + s + "." );

                    line = buffReader.readLine();
                }
            }

            return false; // s is not on the list if list is empty.
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    // remove an item from list
    public void removeAnItem(final String selected) {
        final String toRemove = selected;
        final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(this);
        deleteDialog.setTitle("Remove \"" + toRemove + "\" from " + sl.listname+"." );
        deleteDialog.setMessage("Are you sure?");

        deleteDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // open file
                try {
                    InputStream is = openFileInput(sl.listname + ".txt");
                    File newFile = new File ("toberenamed" + ".txt");
                    String line;
                    FileOutputStream fos = openFileOutput(newFile.getName(), Context.MODE_APPEND);
                    if (is != null) {
                        InputStreamReader inputReader = new InputStreamReader(is);
                        BufferedReader buffReader = new BufferedReader(inputReader);
                        line = buffReader.readLine();
                        while (line != null) {
                            // Write all unmatched item to a new file.
                            if (!line.isEmpty() && !line.contentEquals(toRemove)){
                                fos.write(line.getBytes());
                                fos.write("\n".getBytes());
                            }
                            line = buffReader.readLine();
                        }
                    }
                    fos.flush();
                    fos.close();
                    is.close();
                    debugPrintlistcontent("toberenamed");
                    // must have the actual file object for rename to work!
                    File oldFile = myGetFile(sl.listname);
                    File special = myGetFile("toberenamed");
                    if (oldFile != null) {
                        oldFile.delete();
                        if (special != null && special.exists()) {
                            Boolean status = special.renameTo(oldFile);
                            Log.v("RemoveAnItem:", "rname file status: " + status);
                        }
                        else
                            Log.v("RemoveAnItem:", "rename special doesn't exist?!");
                    }

                } catch(IOException e) {
                    e.printStackTrace();
                }

                listItems();
            }
        });

        deleteDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        deleteDialog.create();
        deleteDialog.show();
    }

    public File myGetFile (String s) {

        File file = getApplicationContext().getFilesDir();
        File [] files = file.listFiles();
        for (File f: files) {
            Log.v ("myGetFile:", f.getName());
            if (f.getName().equalsIgnoreCase(s +".txt")) {
                return f;
            }
        }
        return null;
    }

    public boolean deleteAFile (String s) {
        File file = getApplicationContext().getFilesDir();
        File [] files = file.listFiles();
        for (File f: files) {
            Log.v ("deleteAFile:", f.getName());
            if (f.getName().equalsIgnoreCase(s +".txt")) {
                f.delete();
                return true;
            }
        }
        return false;
    }

    public void debugPrintlistcontent (String s) {

        try {
            InputStream is = openFileInput(s + ".txt");
            if (is != null) {
                InputStreamReader inputReader = new InputStreamReader(is);
                BufferedReader buffReader = new BufferedReader(inputReader);
                String line = buffReader.readLine();
                while (line != null) {
                    Log.v ("debugPrtintListContent", s + " has " + line + ".");
                    line = buffReader.readLine();
                }
                is.close();
            }

        }catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void getAbout() {
        final AlertDialog.Builder aboutDialog = new AlertDialog.Builder(this);
        aboutDialog.setTitle("About List Buddy");
        aboutDialog.setMessage("Copyright (c) 2016.\n\t\tDany madden.\n\t\tGustaf Hegnell.");

        aboutDialog.setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        aboutDialog.create();
        aboutDialog.show();
    }

    public void getHelp() {
        final AlertDialog.Builder helpDialog = new AlertDialog.Builder(this);
        helpDialog.setTitle("Manage List Activity Help:");
        helpDialog.setMessage(menuHelp);

        helpDialog.setNeutralButton("Got it!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        helpDialog.create();
        helpDialog.show();
    }

    public void doExit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            t1.speak("Goodbye!... ", TextToSpeech.QUEUE_FLUSH, null, null);
        } else
            t1.speak("Goodbye!... ", TextToSpeech.QUEUE_FLUSH, null);

        finish();
        System.exit(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            //case R.id.settings:
            //    startActivity(new Intent(this, SettingsActivity.class));
            //    return true;

            case R.id.about:
                getAbout();
                return true;

            case R.id.help:
                getHelp();
                return true;

            case R.id.action_add:
                addAnItem();
                return true;

            case R.id.action_speak:
                promptSpeechInput();
                return true;

            case R.id.count:
                String res = countItem();
                for (int p = 0; p < 2; p++) {
                    Toast.makeText(getApplicationContext(), res, Toast.LENGTH_LONG).show();
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    t1.speak(res, TextToSpeech.QUEUE_FLUSH, null, null);
                } else
                    t1.speak(res, TextToSpeech.QUEUE_FLUSH, null);

                return true;

            case R.id.exit:
                doExit();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (t1!=null) {
            t1.stop();
            t1.shutdown();
        }
        super.onDestroy();
    }
    @Override
    public void onResume(){
        //muteVoice = sharedPrefs.getBoolean("checkBoxVoice", false);
        super.onResume();
    }
}
