package com.rojama.pianoshelf;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TabHost.OnTabChangeListener;

public class PianoShelfActivity extends Activity {
	private static final String TAG = "PianoShelf";
	public TabHost mTabHost = null;
	public TabWidget mTabWidget = null;
	public DatabaseHelper dbhelp;
	TabBrowseList tbl;
	TabRecentList trl;
	TabFavoriteList tfl;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		try {
			dbhelp = new DatabaseHelper(this);

			mTabHost = (TabHost) findViewById(android.R.id.tabhost);
			mTabHost.setup();
			mTabWidget = mTabHost.getTabWidget();
			mTabHost.addTab(mTabHost.newTabSpec("browse").setContent(R.id.tab_browse).setIndicator(
					getText(R.string.tab_browse)));
			mTabHost.addTab(mTabHost.newTabSpec("recent").setContent(R.id.tab_recent).setIndicator(
					getText(R.string.tab_recent)));
			mTabHost.addTab(mTabHost.newTabSpec("favorite").setContent(R.id.tab_favorite)
					.setIndicator(getText(R.string.tab_favorite)));

			LinearLayout ll = (LinearLayout) this.findViewById(R.id.tab_browse);
			tbl = new TabBrowseList(this);
			ll.addView(tbl);
			ll = (LinearLayout) this.findViewById(R.id.tab_recent);
			trl = new TabRecentList(this);
			ll.addView(trl);
			ll = (LinearLayout) this.findViewById(R.id.tab_favorite);
			tfl = new TabFavoriteList(this);
			ll.addView(tfl);

			mTabHost.setOnTabChangedListener(new OnTabChangeListener() {
				@Override
				public void onTabChanged(String tabId) {
					if ("recent".equals(tabId)) {
						trl.getFileDir();
					} else if ("favorite".equals(tabId)) {
						tfl.getFileDir();
					}
				}
			});							
			
			//∫ÛÃ®º”‘ÿ“Ù∑˚
			LoadThread load = new LoadThread();
			load.context = this;
			load.start();			
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public class LoadThread extends Thread
	{
		public Context context;
		public void run()
		{					
			SoundPoolUtiil.loadSound(context);
			System.out.println("Load music complease");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Group 1
		int group1 = 1;
		menu.add(group1, 1, 1, getString(R.string.menu_setting));
		menu.add(group1, 2, 2, getString(R.string.menu_exit));
		// Group 2
		int group2 = 2;
		menu.add(group2, 3, 3, "g2.item1");
		menu.add(group2, 4, 4, "g2.item2");
		// It is important to return true to see the menu
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case 1:
				Intent intent = new Intent();
				intent.setClass(this, AppPreferenceActivity.class);
				startActivity(intent);
				break;
			case 2:
				this.finish();
				break;
		}
		// for items handled
		return true;

	}
	
	@Override
	public void onDestroy(){
		System.exit(0);
	}

}