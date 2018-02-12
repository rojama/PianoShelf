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

public class TabFavoriteList extends ListView implements OnItemClickListener, OnItemLongClickListener{
	private List<String> items = null;
	private List<String> paths = null;
	DatabaseHelper dbhelp;

	public TabFavoriteList(Context context) {
		super(context);
		this.setOnItemClickListener(this);
		this.setOnItemLongClickListener(this);
		dbhelp = ((PianoShelfActivity)context).dbhelp;
		getFileDir();		
	}

	public void getFileDir() {
		try {
			items = new ArrayList<String>();
			paths = new ArrayList<String>();
			for (String item : dbhelp.selectFavoriteItem()) {
				int index;
				if ((index = item.lastIndexOf(File.separatorChar)) != -1){
					items.add(item.substring(index+1));
					paths.add(item);
				}
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
			if (file.isFile()){
				dbhelp.insertRecentItem(file.getPath());
				Intent intent = new Intent();
				intent.setData(Uri.fromFile(file));			
				intent.setClass(this.getContext(), GraphicsActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				this.getContext().startActivity(intent);				
			}else{
				Toast.makeText(this.getContext(), this.getContext().getString(R.string.info_not_exiet), Toast.LENGTH_SHORT).show();
			}
		} else {
			Toast.makeText(this.getContext(), this.getContext().getString(R.string.info_canot_read), Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		dbhelp.deleteFavoriteItem(paths.get(position));
		Toast.makeText(this.getContext(), this.getContext().getString(R.string.info_remove_favorite), Toast.LENGTH_SHORT).show();
		getFileDir();
		return true;
	}


}