package com.rojama.pianoshelf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

/**
 * Tab 3: 收藏夹
 */
public class TabFavoriteList extends ListView implements OnItemClickListener, OnItemLongClickListener {
	private List<String> items = null;
	private List<String> paths = null;
	DatabaseHelper dbhelp;

	public TabFavoriteList(Context context) {
		super(context);
		this.setOnItemClickListener(this);
		this.setOnItemLongClickListener(this);
		dbhelp = ((PianoShelfActivity) context).dbhelp;
		getFileDir();
	}

	public void getFileDir() {
		try {
			items = new ArrayList<String>();
			paths = new ArrayList<String>();
			for (String item : dbhelp.selectFavoriteItem()) {
				int index;
				if ((index = item.lastIndexOf(File.separatorChar)) != -1) {
					items.add(item.substring(index + 1));
					paths.add(item);
				}
			}
			ArrayAdapter<String> fileList = new ArrayAdapter<String>(getContext(),
					android.R.layout.simple_list_item_1, items);
			setAdapter(fileList);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> l, View v, int position, long id) {
		if (position < 0 || position >= paths.size()) return;
		File file = new File(paths.get(position));
		if (!file.exists() || !file.isFile()) {
			Toast.makeText(getContext(), getContext().getString(R.string.info_not_exiet),
					Toast.LENGTH_SHORT).show();
			return;
		}
		if (!file.canRead()) {
			Toast.makeText(getContext(), getContext().getString(R.string.info_canot_read),
					Toast.LENGTH_SHORT).show();
			return;
		}
		dbhelp.insertRecentItem(file.getPath());
		TabBrowseList.launchGraphicsActivity(getContext(), file);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		if (position < 0 || position >= paths.size()) return true;
		dbhelp.deleteFavoriteItem(paths.get(position));
		Toast.makeText(getContext(), getContext().getString(R.string.info_remove_favorite),
				Toast.LENGTH_SHORT).show();
		getFileDir();
		return true;
	}
}
