package com.example.dany.shoppinglist;

import android.content.DialogInterface;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
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

import android.app.ListActivity;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;

import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import java.util.Locale;
import java.lang.CharSequence;

public class MainActivity extends AppCompatActivity {
	TextToSpeech t1;
	CharSequence welcome = "Welcome to List Buddy";
    private SharedPreferences sharedPrefs;
    Boolean muteVoice;
    File [] files;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // check muteVoice
        PreferenceManager.setDefaultValues(this, R.xml.fragment_settings, false);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        muteVoice = sharedPrefs.getBoolean("checkBoxVoice", false);

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

        doShowLists();
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
                try {
                    FileOutputStream fos = openFileOutput(fname + ".txt", Context.MODE_PRIVATE);
                    fos.write("hello".getBytes());
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
                createAList();
                return true;

            case R.id.exit:
                if (!muteVoice) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        t1.speak("Goodbye!... ", TextToSpeech.QUEUE_FLUSH, null, null);
                    } else
                        t1.speak("Goodbye!... ", TextToSpeech.QUEUE_FLUSH, null);
                }
                finish();
                System.exit(0);

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
	public void onPause() {
		if (t1!=null) {
			t1.stop();
			t1.shutdown();
		}
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
        muteVoice = sharedPrefs.getBoolean("checkBoxVoice", false);
        super.onResume();
    }

    public void doShowLists() {

        // list all /data/user/0/com.example.dany.shoppinglist/files
        File file = this.getFilesDir();
        files = file.listFiles();
        List <ShoppingList> myShoppingLists = new ArrayList <ShoppingList>();
        int numOfFiles = files.length;

        // we should only count .txt files. but close enough...
        String [] tmp = file.list();
        List<String> tmp2 = new ArrayList<String>(Arrays.asList(tmp));
        int j = 0;
		int pos = 0;

		// this list holds all .txt files. They're the lists.
        List<String> SHOPPINGLIST= new ArrayList<String>(tmp2.size());

        // get just the txt files and be sure to remove the .txt extension.
        for (String s : tmp2) {
            if (s.endsWith(".txt")) {
				pos = s.lastIndexOf(".");
				if (pos > 0)
                	SHOPPINGLIST.add(s.substring(0,pos));
			}
        }
        Collections.sort(SHOPPINGLIST); // Alphabetize the list.

        ArrayAdapter adapter = new ArrayAdapter<String>(this,R.layout.content_main,SHOPPINGLIST);
        ListView listView = (ListView) findViewById(R.id.list_list);
        listView.setAdapter(adapter);

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
                String selected = ((TextView)view).getText().toString();
                removeList(selected);

                return true;
            }
        });
    }

    // remove list from App
    public void removeList(final String selected) {
        final String toRemove = selected;
        File file = this.getFilesDir();
        files = file.listFiles();
        final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(this);
        deleteDialog.setTitle("Delete " + selected + " list?" );
        deleteDialog.setMessage("You will lost all of its content.");

        deleteDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                for (int p = 0; p < files.length; p++) {
                    if (files[p].getName().contains(selected)) {
                        if (!files[p].delete())
                            Toast.makeText(getApplicationContext(), "Couldn't delete" + files[p].getName(), Toast.LENGTH_LONG).show();
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
        b.putBoolean("muteVoice", muteVoice);
        i.putExtras(b);
        //i.putExtra("files", files);

        startActivity(i);
    }
}
