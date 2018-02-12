package com.rojama.pianoshelf;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class AppPreferenceActivity extends PreferenceActivity {	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// --load the preferences from an XML file---
		PreferenceManager prefMgr = getPreferenceManager();
		prefMgr.setSharedPreferencesName("appPreferences");
		addPreferencesFromResource(R.xml.perference);
	}
}
