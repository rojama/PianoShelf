package com.rojama.pianoshelf;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;

public class GraphicsActivity extends Activity {
	private GraphicsView graphicsView = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		try {
			super.onCreate(savedInstanceState);
			Intent intent = getIntent();
			File file = new File(new URI(intent.getData().toString()));
			String filepath = file.getPath();	
			setContentView(R.layout.shelf);
			graphicsView = new GraphicsView(GraphicsActivity.this);
			graphicsView.filepath = filepath;
			Display display = getWindowManager().getDefaultDisplay();
			graphicsView.screenWidth = display.getWidth();
			graphicsView.screenHeight = display.getHeight();
			graphicsView.showView();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Group 1
		int group1 = 1;
		menu.add(group1, 1, 1, getString(R.string.menu_pageup));
		menu.add(group1, 2, 2, getString(R.string.menu_pagedown));
		menu.add(group1, 3, 3, getString(R.string.menu_play));
		// Group 2
		// int group2 = 2;
		// menu.add(group2, 3, 3, "g2.item1");
		// menu.add(group2, 4, 4, "g2.item2");
		// It is important to return true to see the menu
		return true;
	}

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		if (graphicsView != null && graphicsView.dispalyPageNo <= 1) {
			menu.getItem(0).setEnabled(false);
		} else {
			menu.getItem(0).setEnabled(true);
		}

		if (graphicsView != null && graphicsView.dispalyPageNo >= graphicsView.ct.maxPage) {
			menu.getItem(1).setEnabled(false);
		} else {
			menu.getItem(1).setEnabled(true);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case 1:
			if (graphicsView != null && graphicsView.dispalyPageNo > 1) {
				graphicsView.olddispalyPageNo = graphicsView.dispalyPageNo;
				graphicsView.dispalyPageNo--;
				graphicsView.reShowView();
			}
			break;
		case 2:
			if (graphicsView != null && graphicsView.dispalyPageNo < graphicsView.ct.maxPage) {
				graphicsView.olddispalyPageNo = graphicsView.dispalyPageNo;
				graphicsView.dispalyPageNo++;
				graphicsView.reShowView();
			}
			break;
		case 3:
			if (graphicsView != null) {
				graphicsView.play();
			}
			break;
		}
		
		// for items handled
		return true;

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Display display = getWindowManager().getDefaultDisplay();
		graphicsView.screenWidth = display.getWidth();
		graphicsView.screenHeight = display.getHeight();
		graphicsView.changeOrientation();
		super.onConfigurationChanged(newConfig);
	}
}
