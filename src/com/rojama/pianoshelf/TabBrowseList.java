package com.rojama.pianoshelf;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.ContextMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class TabBrowseList extends ListView implements OnItemClickListener, OnItemLongClickListener {
	private List<String> items = null;
	private List<String> paths = null;
	private String rootpath = "/";
	DatabaseHelper dbhelp;

	public TabBrowseList(Context context) {
		super(context);
		this.setOnItemClickListener(this);
		this.setOnItemLongClickListener(this);
		dbhelp = ((PianoShelfActivity) context).dbhelp;
		getFileDir(rootpath);
	}

	public void getFileDir() {
		getFileDir(rootpath);
	}

	public void getFileDir(String filePath) {
		try {
			items = new ArrayList<String>();
			paths = new ArrayList<String>();
			File f = new File(filePath);
			File[] files = f.listFiles(new MusicFileFilter());

			if (!filePath.equals(rootpath)) {
				items.add(this.getContext().getString(R.string.dis_back_root));
				paths.add(rootpath);
				items.add(String.format(this.getContext().getString(R.string.dis_back_up), f
						.getParent()));
				paths.add(f.getParent());
			}
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				String itemsname = file.getName();
				if (file.isDirectory()) {
					itemsname = "[" + itemsname + "]";
				}
				items.add(itemsname);
				paths.add(file.getPath());
			}
			ArrayAdapter<String> fileList = new ArrayAdapter<String>(this.getContext(),
					android.R.layout.simple_list_item_1, items);
			setAdapter(fileList);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> l, View v, int position, long id) {
		File file = new File(paths.get(position));
		if (file.canRead()) {
			if (file.isDirectory()) {
				getFileDir(paths.get(position));
			} else {
				dbhelp.insertRecentItem(file.getPath());
				Intent intent = new Intent();
				intent.setData(Uri.fromFile(file));				
				intent.setClass(this.getContext(), GraphicsActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				this.getContext().startActivity(intent);
			}
		} else {
			Toast.makeText(this.getContext(),
					this.getContext().getString(R.string.info_canot_read), Toast.LENGTH_SHORT)
					.show();
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		File file = new File(paths.get(position));
		if (file.canRead()) {
			if (file.isFile()) {
				dbhelp.insertFavoriteItem(file.getPath());
				Toast.makeText(this.getContext(),
						this.getContext().getString(R.string.info_add_favorite), Toast.LENGTH_SHORT)
						.show();
			}
		} else {
			Toast.makeText(this.getContext(),
					this.getContext().getString(R.string.info_canot_read), Toast.LENGTH_SHORT)
					.show();
		}
		return true;
	}


	public class MusicFileFilter implements FileFilter {

		@Override
		public boolean accept(File file) {
			if (!file.canRead()) {
				return false;
			}

			String filename = file.getName().toUpperCase();

			if (filename.startsWith(".") || filename.equals("LOST.DIR") || filename.equals("DCIM")) {
				return false;
			}

			if (file.isDirectory()) {
				return true;
			}

			if (filename.endsWith(".XML") || filename.endsWith(".MXL")) {
				return true;
			} else {
				return false;
			}

		}
	}

}