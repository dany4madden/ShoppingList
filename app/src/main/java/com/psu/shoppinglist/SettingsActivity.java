package com.psu.shoppinglist;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

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
