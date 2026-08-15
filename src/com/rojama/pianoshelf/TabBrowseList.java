package com.rojama.pianoshelf;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import androidx.core.content.FileProvider;

/**
 * Tab 1: 文件浏览器
 *
 * 修复:
 *  - [FileUriExposedException] Android 7.0+ 禁止 app 间暴露 file:// URI
 *    → 统一使用 FileProvider.getUriForFile() + FLAG_GRANT_READ_URI_PERMISSION
 *  - 启动 Intent 增加 FLAG_ACTIVITY_NEW_TASK 仅在非 Activity Context 时才必须
 *  - MusicFileFilter 提取为 static
 */
public class TabBrowseList extends ListView implements OnItemClickListener, OnItemLongClickListener {
	private static final String FILEPROVIDER_AUTH = "com.rojama.pianoshelf.fileprovider";

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
				items.add(String.format(this.getContext().getString(R.string.dis_back_up),
						f.getParent() == null ? rootpath : f.getParent()));
				paths.add(f.getParent() == null ? rootpath : f.getParent());
			}
			if (files != null) {
				for (File file : files) {
					String itemsname = file.getName();
					if (file.isDirectory()) itemsname = "[" + itemsname + "]";
					items.add(itemsname);
					paths.add(file.getPath());
				}
			}
			ArrayAdapter<String> fileList = new ArrayAdapter<String>(this.getContext(),
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
		if (!file.canRead()) {
			Toast.makeText(getContext(), getContext().getString(R.string.info_canot_read),
					Toast.LENGTH_SHORT).show();
			return;
		}
		if (file.isDirectory()) {
			getFileDir(paths.get(position));
		} else if (file.isFile()) {
			dbhelp.insertRecentItem(file.getPath());
			launchGraphicsActivity(getContext(), file);
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		if (position < 0 || position >= paths.size()) return true;
		File file = new File(paths.get(position));
		if (file.canRead() && file.isFile()) {
			dbhelp.insertFavoriteItem(file.getPath());
			Toast.makeText(getContext(), getContext().getString(R.string.info_add_favorite),
					Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(getContext(), getContext().getString(R.string.info_canot_read),
					Toast.LENGTH_SHORT).show();
		}
		return true;
	}

	/** Safe cross-version launcher for GraphicsActivity via FileProvider. */
	static void launchGraphicsActivity(Context context, File file) {
		try {
			Uri uri = FileProvider.getUriForFile(context, FILEPROVIDER_AUTH, file);
			Intent intent = new Intent();
			intent.setDataAndType(uri, "application/vnd.recordare.musicxml");
			intent.setClass(context, GraphicsActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
			context.startActivity(intent);
		} catch (Throwable t) {
			// Fallback for unusual OEM FileProvider issues (rare)
			Toast.makeText(context, context.getString(R.string.info_canot_read),
					Toast.LENGTH_SHORT).show();
		}
	}

	/** Only directories + .xml/.mxl files + no hidden dirs. */
	public static class MusicFileFilter implements FileFilter {
		@Override
		public boolean accept(File file) {
			if (!file.canRead()) return false;
			String name = file.getName();
			if (name.startsWith(".") || "LOST.DIR".equals(name) || "DCIM".equals(name)) {
				return false;
			}
			if (file.isDirectory()) return true;
			String upper = name.toUpperCase();
			return upper.endsWith(".XML") || upper.endsWith(".MXL");
		}
	}
}
