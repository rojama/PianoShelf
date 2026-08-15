package com.rojama.pianoshelf;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TabHost.OnTabChangeListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * 主界面 (入口 Activity)
 *
 * 修复 / Modernization:
 *  - 继承 AppCompatActivity 以便使用 AndroidX Toolbar/Lifecycle
 *  - 移除 onDestroy 中的 System.exit(0)（违反 Android 生命周期/任务栈设计）
 *  - onDestroy 显式释放 SoundPool 资源
 *  - Android 6.0+ 动态申请外部存储读写权限（否则 Browse/Recent/Favorite 全部为空）
 */
public class PianoShelfActivity extends AppCompatActivity {
	private static final String TAG = "PianoShelf";
	private static final int REQ_STORAGE = 1000;
	public TabHost mTabHost = null;
	public TabWidget mTabWidget = null;
	public DatabaseHelper dbhelp;
	TabBrowseList tbl;
	TabRecentList trl;
	TabFavoriteList tfl;

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

			// 后台加载音符资源（耗时）
			LoadThread load = new LoadThread();
			load.context = this;
			load.start();

			// 动态请求存储权限 (Android 6.0+)
			requestStoragePermissionIfNeeded();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void requestStoragePermissionIfNeeded() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
		int r = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
		if (r != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE },
					REQ_STORAGE);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == REQ_STORAGE && grantResults.length > 0) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				// 刷新列表显示
				if (tbl != null) tbl.getFileDir();
				if (trl != null) trl.getFileDir();
				if (tfl != null) tfl.getFileDir();
			}
		}
	}

	public class LoadThread extends Thread {
		public android.content.Context context;
		public void run() {
			SoundPoolUtiil.loadSound(context);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		int group1 = 1;
		menu.add(group1, 1, 1, getString(R.string.menu_setting));
		menu.add(group1, 2, 2, getString(R.string.menu_exit));
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
				// Graceful finish; avoid System.exit to respect Android task stack.
				finishAffinity();
				break;
		}
		return true;
	}

	@Override
	public void onDestroy() {
		SoundPoolUtiil.release();
		if (dbhelp != null) {
			try { dbhelp.close(); } catch (Throwable ignored) {}
			dbhelp = null;
		}
		super.onDestroy();
	}
}
