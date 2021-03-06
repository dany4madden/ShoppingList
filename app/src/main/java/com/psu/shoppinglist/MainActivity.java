package com.psu.shoppinglist;

import android.content.DialogInterface;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;

import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import java.util.Locale;
import java.lang.CharSequence;
import java.util.Map;

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

public class MainActivity extends AppCompatActivity {
	TextToSpeech t1;
	CharSequence welcome = "Welcome to List Buddy.";

    String mainMenuHelp = "This is the Main Activity page. \n\nYou can Create, Delete, Count, or Open any existing list by saying " +
                    "following commands: " +
                    "\n\n\t- Create a Safeway list." +
                    "\n\t- Delete a Safeway list." +
                    "\n\t- Open a Safeway list." +
                    "\n\t- Count lists." +
                    "\n\nThese functions are also accessible from the overflow menu.";

    //private SharedPreferences sharedPrefs;
    //Boolean muteVoice;
    File [] files;
    private static final int SPEECH_REQUEST_CODE = 0;
    int numList = 0;
    boolean guided = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        doShowLists();
        initializeSpeech();
    }

    public void initializeSpeech () {
        // the welcome speech
        t1 = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = t1.setLanguage(Locale.US);

                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    }
                    Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_SHORT).show();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        t1.speak(welcome, TextToSpeech.QUEUE_FLUSH, null, null);
                    } else {
                        t1.speak(welcome.toString(), TextToSpeech.QUEUE_FLUSH, null);
                    }

                    if (guided) {
                        int a = 0;
                        while (a < 5) {
                            Toast.makeText(getApplicationContext(), "I am here to help you create and manage your grocery lists. "  +
                                    "Let's start by creating a list. \n\nFor example: to create a Costco list, " +
                                    "click the speech icon and say: \n - Create Costco list. \n\nThen, once the list is created " +
                                    "you can manage it by saying: \n - Open Costco list.", Toast.LENGTH_LONG).show();
                            a++;
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            t1.speak("I am here to help you create and manage your grocery lists. " +
                                    "Let's start by creating a list. For example: to create a Costco list, " +
                                    "click the speech icon and say: Create Costco list. Then, once the list is created" +
                                             " you can manage it by saying: Open Costco list.",
                                    TextToSpeech.QUEUE_ADD, null, null);
                        } else {
                            t1.speak("I am here to help you create and manage your grocery lists. " +
                                    "Let's start by creating a list. For example: to create a Costco list, " +
                                    "click the speech icon and say: Create Costco list. Then, once the list is created " +
                                            " you can manage it by saying: Open Costco list.\",",
                            TextToSpeech.QUEUE_ADD, null);
                        }
                    }

                } else
                    Log.v("DEBUG", "TTS failed to start");
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

        //Log.v ("----API req:", speech + ".");
        final AIRequest aiRequest = new AIRequest();
        final AIConfiguration config = new AIConfiguration("053305c5da7a46f0945605cbb496188e",
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
                    Boolean noToast = false;
                    // Get parameters
                    String parameterString = "";
                    if (result.getParameters() != null && !result.getParameters().isEmpty()) {
                        for (final Map.Entry<String, JsonElement> entry : result.getParameters().entrySet()) {
                            parameterString += "(" + entry.getKey() + ", " + entry.getValue() + ") ";
                        }
                    }

                    // Need to decide what to do with our response:
                    String intent = result.getMetadata().getIntentName();
                    String response = " ";
                    //Log.v ("-----------API: ", intent + ".");

                    if (intent == null) {
                        // making use of API.IA's cleverness!
                        response = result.getFulfillment().getSpeech();
                        if (response.isEmpty())
                            response = "I didn't get what you wanted me to do.";
                    } else {
                        switch (intent) {
                            case "addList":
                                response = vCreateAList(result);
                                noToast = false;
                                break;
                            case "removeList":
                                response = vRemoveAList(result);
                                doShowLists();
                                noToast = false;
                                break;
                            case "count":
                                response = vCountList(result);
                                noToast = false;
                                break;
                            case "openList":
                                response = vOpenList(result);
                                noToast = true;
                                break;
                            case "addItem":
                                response = vAddItem(result);
                                noToast = true;
                                break;
                            case "help":
                                getHelp();
                                response = "Main Activity. You can Create, Delete, Count, or Open any existing list by saying one" +
                                        " of the following commands: " +
                                            "- Create Safeway list\n" +
                                            "- Add Apples to Safeway list\n" +
                                            "- Delete Safeway list\n" +
                                            "- Open Safeway list\n" +
                                            "- Count lists \n" +
                                            "- Or Exit.\n";
                                noToast = true;
                                break;
                            case "exit":
                                doExit();
                                break;
                            case "about":
                                getAbout();
                                // Need to make sure that the text to speech pronounces names correctly.
                                response = "This app is developed by: Danny Madden and Gustaf Hegnell.";
                                noToast = true;
                                break;

                            case "goBackMain":
                                response = "I can not back up anymore. This is the main activity page. If you want to quit say Exit.";
                                noToast = false;
                                break;
                            default:
                                response = "I heard: " + result.getResolvedQuery() +". I don't know what to do with that." ;
                        }
                    }
                    if (noToast == false) {
                        if (!response.contentEquals(" ")) {
                            for (int p = 0; p < 2; p++) {
                                Toast.makeText(getApplicationContext(), response, Toast.LENGTH_LONG).show();
                            }
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

    public boolean fileExists (String name){
        File f = this.getFilesDir();
        File [] files = f.listFiles();

        for (File a: files) {
            if (a.getName().contentEquals(name+".txt"))
                return true;
        }
        return false;
    }

    public String vOpenList(Result result) {
        String respond = "";
        String listName = result.getStringParameter("listName");

        // trim and capitalized listName
        if (!listName.isEmpty()) {
            listName = listName.trim();
            String fname = listName.substring(0, 1).toUpperCase() +
                    listName.substring(1);
            // check if list exist.
            if (fileExists(fname)) {
                showListItems(fname);
            } else {
                respond = "Open " + listName + " list failed. You need to create it first.";
            }
        }
        return respond;
    }

    public String vAddItem(Result result) {
        String response = "";
        String listName = result.getStringParameter("listName");

        if (listName.isEmpty()) {
            return "You did not provide a list to add that item to.";
        } else {
            Bundle b = new Bundle();
            Intent i = new Intent(this, ManageListActivity.class);

            if (!fileExists(listName)) {
                try {
                    String fname = listName.substring(0, 1).toUpperCase() +
                            listName.substring(1);
                    FileOutputStream fos = openFileOutput(fname + ".txt", Context.MODE_PRIVATE);
                    fos.close();
                    doShowLists();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            b.putString("listName", listName);
            i.putExtras(b);
            i.putExtra("methodName","addItem");
            i.putExtra("item", result.getStringParameter("itemName"));

            startActivity(i);
        }
        return response;
    }

    public String vCountList (Result result) {
        String respond = "";
        if (numList == 0)
            respond = "You do not have any lists.";
        else
            respond = "You have " + numList +" lists total. To hear the number of items on a particular list, open up the list first.";
        return respond;
    }

    public String vRemoveAList (Result result) {
        String listName = result.getStringParameter("listName");
        String itemName = result.getStringParameter("itemName");
        String respond = listName + " does not exist.";

        if (!itemName.isEmpty()) {
            respond = "I am not that advanced yet. To delete an item from the list, you must open up the list first.";
        }
        else {
            if (!listName.isEmpty()) {
                File f = this.getFilesDir();
                File[] files = f.listFiles();
                listName = listName.trim();
                String fname = listName.substring(0, 1).toUpperCase() +
                        listName.substring(1);
                //debug_print_list_file();

                for (File a : files) {
                    if (a.getName().contentEquals(fname+".txt")) {
                        if (!a.delete()) {
                            respond = "Unable to delete " + "fname" + " list";
                        } else {
                            respond = "Ok, " + fname + " list has been deleted.";
                        }

                    } else {
                        //respond = "Hmmmmm. I am having trouble with this one. I can't find a " + fname + " list.";
                        respond = "Ok. " + fname + " list is deleted.";
                    }
                }
            }
            else {
                respond = "I didn't get a list name.";
            }
        }
        return respond;
    }

    // Requires that ai result contain "listName"
    public String vCreateAList (Result result){
        String listName = result.getStringParameter("listName");
        String itemName = result.getStringParameter("itemName");
        String respond = "I was not able to create a list as you requested.";

        // App limitation. If item is given also, then user have to open the list first before
        // they can add an item to the list.
        if (!itemName.isEmpty()) {
            respond = "I'm not that advanced yet. You can add an item once you've opened up the list.";
        }
        else {
            if (!listName.isEmpty()) {
                listName = listName.trim();
                // check if list is already existed
                if (fileExists(listName)) {
                    respond = "A " + listName + " list already exists.";
                } else {
                    try {
                        String fname = listName.substring(0, 1).toUpperCase() +
                                listName.substring(1);
                        FileOutputStream fos = openFileOutput(fname + ".txt", Context.MODE_PRIVATE);
                        fos.close();
                        doShowLists();
                        respond = "A " + fname + " list has been created.";
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else
                respond = "List creation failed.";
        }
        return respond;
    }

    public void getHelp() {
        final AlertDialog.Builder helpDialog = new AlertDialog.Builder(this);
        helpDialog.setTitle("List Buddy Help:");
        helpDialog.setMessage(mainMenuHelp);

        helpDialog.setNeutralButton("Got it!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        helpDialog.create();
        helpDialog.show();
    }

    public void doExit(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            t1.speak("Goodbye!... ", TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            t1.speak("Goodbye!... ", TextToSpeech.QUEUE_FLUSH, null);
        }
        while (t1.isSpeaking()) {
            // wait...
        }
        finish();
        System.exit(0);

    }

    public void getAbout() {
        final AlertDialog.Builder aboutDialog = new AlertDialog.Builder(this);
        aboutDialog.setTitle("About List Buddy");
        aboutDialog.setMessage("Copyright (c) 2016.\n\t\tDany Madden.\n\t\tGustaf Hegnell.\n");

        aboutDialog.setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        aboutDialog.create();
        aboutDialog.show();
    }
    public void debug_print_list_file() {
        File f = this.getFilesDir();
        File [] files = f.listFiles();

        for (File a: files) {
            Log.v ("debug_print_file", a.getName() + ".");
        }
    }

    public String countList () {
        String respond;
        if (numList == 0)
            respond = "You do not have any lists.";
        else
            respond = "You have " + numList +" lists total. To hear the number of item on each list, open up the list first.";
        return respond;
    }

    // Adding a new list
    public void createAList() {
        final AlertDialog.Builder addDialog = new AlertDialog.Builder(this);
        addDialog.setTitle("Add a new list");
        final EditText input = new EditText(this);
        addDialog.setView(input);


        addDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String inputString = input.getText().toString();
                // create a text file by the inputString name. Capitalize the first letter
                String fname = inputString.substring(0,1).toUpperCase() +
                        inputString.substring(1);
                fname = fname.trim();
                String respond;
                try {
                    FileOutputStream fos = openFileOutput(fname + ".txt", Context.MODE_PRIVATE);
                    //fos.write("hello".getBytes());
                    fos.close();
                    doShowLists();
                } catch (IOException e) {
                    e.printStackTrace();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String response = "";
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.about:
                getAbout();
                return true;

            case R.id.help:
                getHelp();
                return true;

            case R.id.action_add:
                createAList();
                return true;

            case R.id.action_speak:
                promptSpeechInput();
                return true;

            case R.id.exit:
                doExit();
                return true;

            case R.id.count:
                String res = countList();

                for (int p = 0; p < 2; p++) {
                    Toast.makeText(getApplicationContext(), res, Toast.LENGTH_LONG).show();
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    t1.speak(res, TextToSpeech.QUEUE_FLUSH, null, null);
                } else
                    t1.speak(res, TextToSpeech.QUEUE_FLUSH, null);
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
        super.onResume();
    }

    public void doShowLists() {

        // list all /data/user/0/com.psu.shoppinglist/files
        File file = this.getFilesDir();
        // we should only count .txt files. but close enough...
        String [] tmp = file.list();
        List<String> tmp2 = new ArrayList<String>(Arrays.asList(tmp));
		int pos = 0;
        int counted = 0;

		// this list holds all .txt files. They're the lists.
        List<String> SHOPPINGLIST= new ArrayList<String>(tmp2.size());

        // get just the txt files and be sure to remove the .txt extension.
        for (String s : tmp2) {
            if (s.endsWith(".txt")) {
				pos = s.lastIndexOf(".");
				if (pos > 0)
                	SHOPPINGLIST.add(s.substring(0,pos));
                    counted ++;
			}
        }
        Collections.sort(SHOPPINGLIST); // Alphabetize the list.
        numList = counted;

        if (SHOPPINGLIST.isEmpty()) {
            SHOPPINGLIST.add("no list");
            guided = true;
        }

        ArrayAdapter adapter = new ArrayAdapter<String>(this,R.layout.content_main,SHOPPINGLIST);
        ListView listView = (ListView) findViewById(R.id.list_list);
        listView.setAdapter(adapter);
        if (!SHOPPINGLIST.contains("no list")) {
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    // Open file (list) and create a ShoppingList and display items.
                    String listName = ((TextView) view).getText().toString();

                    showListItems(listName);

                    // Toast.makeText(getApplicationContext(), ((TextView) view).getText(), Toast.LENGTH_LONG).show();
                }
            });

            // long click we remove the list from app
            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                    String selected = ((TextView) view).getText().toString();
                    removeList(selected);

                    return true;
                }
            });
            // debug_print_list_file();
        }
    }

    // remove list from App
    public void removeList(final String selected) {
        final String toRemove = selected;
        File file = this.getFilesDir();
        files = file.listFiles();
        final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(this);
        deleteDialog.setTitle("Delete " + selected + " list?" );
        deleteDialog.setMessage("You will lose all of its content.");

        deleteDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                for (int p = 0; p < files.length; p++) {
                    if (files[p].getName().contains(selected)) {
                        if (!files[p].delete())
                            Toast.makeText(getApplicationContext(), "Could not delete" + files[p].getName(), Toast.LENGTH_LONG).show();
                        else
                            doShowLists();
                    }
                }
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
    // create a ShoppingList and shows its content.
    public void showListItems(String listName) {
        Bundle b = new Bundle();
        Intent i = new Intent(this, ManageListActivity.class);

        b.putString("listName", listName);
        //b.putBoolean("muteVoice", muteVoice);
        i.putExtras(b);
        //i.putExtra("files", files);

        startActivity(i);
    }
}
