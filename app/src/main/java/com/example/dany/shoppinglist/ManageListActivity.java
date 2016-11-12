package com.example.dany.shoppinglist;

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
import android.view.MenuItem;
import android.view.View;
import java.io.File;

public class ManageListActivity extends AppCompatActivity {
    boolean muteVoice;
    String listName;
    ShoppingList sl;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        muteVoice = getIntent().getBooleanExtra("muteVoice", false);
        listName = getIntent().getStringExtra("listName");
        sl = new ShoppingList(listName, null);

        // read the list's content
        listItems(sl);

    }

    // Read in the file list
    public void listItems(ShoppingList sl) {

    }

    // display the list

    // add to list

    // remove from list

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
