package com.example.dany.shoppinglist;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import java.util.List;

/**
 * Created by dany on 11/4/16.
 */
public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle SavedInstanceState){
        super.onCreate(SavedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment())
               .commit();
    }

    public static class SettingsFragment extends PreferenceFragment {

        public boolean muteVoice = false;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.fragment_settings);
        }


    }

}
