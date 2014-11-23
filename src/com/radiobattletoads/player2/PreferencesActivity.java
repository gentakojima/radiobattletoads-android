package com.radiobattletoads.player2;

import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;

public class PreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        
    }
	
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Log.d("RBT", "Called onSharedPreferenceChanged");
		
	}


}