package com.rojama.pianoshelf;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

/**
 * 设置界面 - Modernized (AndroidX PreferenceFragment)
 *
 * 修复:
 *  - PreferenceActivity + addPreferencesFromResource 在 API 11+ 就 deprecated，
 *    替换为 AppCompatActivity + PreferenceFragmentCompat 组合模式
 *  - 使用 androidx.preference 库，保持 Material Design 风格
 */
public class AppPreferenceActivity extends AppCompatActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			getSupportFragmentManager()
					.beginTransaction()
					.replace(android.R.id.content, new SettingsFragment())
					.commit();
		}
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setTitle(R.string.per_display);
		}
		// Make sure SharedPreferences name is consistent across legacy & new code
		PreferenceManager.getDefaultSharedPreferences(this);
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return true;
	}

	public static class SettingsFragment extends PreferenceFragmentCompat {
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			// The SharedPreferences name used everywhere is "appPreferences".
			PreferenceManager preferenceManager = getPreferenceManager();
			preferenceManager.setSharedPreferencesName("appPreferences");
			addPreferencesFromResource(R.xml.perference);
		}
	}
}
