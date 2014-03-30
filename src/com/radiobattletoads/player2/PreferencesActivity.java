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
		if(key=="notification"){
			Log.d("RBT", "onSharedPreferenceChanged: notification");
			Boolean enabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("notification", true);
			if(enabled){
				RBTPlayerApplication.getFromContext(this.getBaseContext()).getNotifications().enable();
			}
			else{
				RBTPlayerApplication.getFromContext(this.getBaseContext()).getNotifications().disable();				
			}
			
		}
		
	}


}