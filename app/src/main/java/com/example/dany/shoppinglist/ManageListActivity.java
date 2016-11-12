package com.example.dany.shoppinglist;

import android.app.ActionBar;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ManageListActivity extends AppCompatActivity {
    boolean muteVoice;
    String listName;
    ShoppingList sl;
    final int MAX_NUM_ITEMS = 50; //allow a max of 50 items on the list!
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_list);
        muteVoice = getIntent().getExtras().getBoolean("muteVice", false);
        listName = getIntent().getExtras().getString("listName", "Error");
        sl = new ShoppingList(listName, null);

        setTitle(listName);
        // read the list's content
        listItems();
    }
    // Read in the file list
    public void listItems() {
        List<String> ITEMLIST= new ArrayList<String>(MAX_NUM_ITEMS);
        String line;

        try {
            InputStream is = openFileInput(sl.listname + ".txt");
            if (is != null) {
                InputStreamReader inputReader = new InputStreamReader(is);
                BufferedReader buffReader = new BufferedReader(inputReader);
                do {
                    line = buffReader.readLine();
                    if (line != null && !line.isEmpty()) {
                        ITEMLIST.add(line);
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

    // display the list

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.about:
                final AlertDialog.Builder aboutDialog = new AlertDialog.Builder(this);
                aboutDialog.setTitle("About List Buddy");
                aboutDialog.setMessage("Copyright (c) 2016.\nGustaf Hegnell and Dany madden.\n");

                aboutDialog.setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                aboutDialog.create();
                aboutDialog.show();

                return true;

            case R.id.action_add:
                addAnItem();
                return true;

            case R.id.exit:
               /* if (!muteVoice) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        t1.speak("Goodbye!... ", TextToSpeech.QUEUE_FLUSH, null, null);
                    } else
                        t1.speak("Goodbye!... ", TextToSpeech.QUEUE_FLUSH, null);
                }"*/
                finish();
                System.exit(0);

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
