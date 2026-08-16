package com.rojama.pianoshelf;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.DocumentsContract.Document;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import androidx.core.content.FileProvider;

/**
 * Tab 1: 文件浏览器
 *
 * 三种数据来源：
 *  1) File API 浏览（有 MANAGE_EXTERNAL_STORAGE 权限时进入 /storage/emulated/0）
 *  2) SAF 目录树浏览（用户点「选择文件夹…」授权后递归列出 .xml/.mxl）—— 推荐，跨 Android 版本最稳
 *  3) 单文件打开（点「打开乐谱文件…」直接进 GraphicsActivity）
 *
 * 修复:
 *  - FileUriExposedException: 统一 FileProvider
 *  - 空白页：默认 File 模式在无权限/无乐谱时给出引导提示，并提供 SAF 文件夹导入入口
 */
public class TabBrowseList extends ListView implements OnItemClickListener, OnItemLongClickListener {
	private static final String FILEPROVIDER_AUTH = "com.rojama.pianoshelf.fileprovider";
	private static final int MAX_DOC_FILES = 500;
	private static final int MAX_DOC_DEPTH = 8;

	private List<String> items = null;
	private List<String> paths = null;
	private String rootpath = "/";
	private boolean rootMode = false;

	// SAF 目录树模式
	private boolean docMode = false;
	private List<Uri> docUris = null;

	DatabaseHelper dbhelp;

	public TabBrowseList(Context context) {
		super(context);
		this.setOnItemClickListener(this);
		this.setOnItemLongClickListener(this);
		dbhelp = ((PianoShelfActivity) context).dbhelp;
		addHeaderView(buildActionHeader());
		getFileDir(rootpath);
	}

	// ==================== 顶部操作栏（SAF 入口） ====================

	private View buildActionHeader() {
		final Context ctx = getContext();
		LinearLayout wrap = new LinearLayout(ctx);
		wrap.setOrientation(LinearLayout.VERTICAL);
		int padH = dp(12), padV = dp(8);
		wrap.setPadding(padH, padV, padH, padV);

		Button btnDir = new Button(ctx);
		btnDir.setText(R.string.browse_btn_open_dir);
		btnDir.setAllCaps(false);
		btnDir.setOnClickListener(v -> {
			if (ctx instanceof PianoShelfActivity) {
				((PianoShelfActivity) ctx).openSystemFilePickerForMusicXml(
						PianoShelfActivity.REQ_OPEN_MUSICXML_TREE);
			}
		});
		wrap.addView(btnDir, lpFill());

		Button btnFile = new Button(ctx);
		btnFile.setText(R.string.browse_btn_open_file);
		btnFile.setAllCaps(false);
		btnFile.setOnClickListener(v -> {
			if (ctx instanceof PianoShelfActivity) {
				((PianoShelfActivity) ctx).openSystemFilePickerForMusicXml(
						PianoShelfActivity.REQ_OPEN_MUSICXML_FILE);
			}
		});
		wrap.addView(btnFile, lpFill());
		return wrap;
	}

	private int dp(int v) {
		return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
	}

	private LinearLayout.LayoutParams lpFill() {
		return new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
	}

	public void getFileDir() {
		getFileDir(rootpath);
	}

	// ==================== SAF 目录树导入 ====================

	/**
	 * 用户通过 ACTION_OPEN_DOCUMENT_TREE 选了一个文件夹后回调。
	 * 后台递归扫描其中所有 .xml / .mxl 文件并刷新列表。
	 */
	public void bindToDocumentTree(Uri treeUri) {
		this.docMode = true;
		this.docUris = null;
		// 立即显示扫描中
		items = new ArrayList<>();
		items.add(getContext().getString(R.string.browse_scanning));
		paths = new ArrayList<>();
		paths.add("");
		setAdapter(new ArrayAdapter<>(getContext(),
				android.R.layout.simple_list_item_1, items));

		final Context ctx = getContext();
		new Thread(() -> {
			List<Uri> result = scanMusicXmlFiles(treeUri);
			runOnUi(() -> {
				docUris = result;
				refreshDocList();
			});
		}, "doc-scan").start();
	}

	private List<Uri> scanMusicXmlFiles(Uri treeUri) {
		List<Uri> out = new ArrayList<>();
		ContentResolver cr = getContext().getContentResolver();
		try {
			String treeDocId = DocumentsContract.getTreeDocumentId(treeUri);
			collectDocs(cr, treeUri, treeDocId, out, 0);
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return out;
	}

	private void collectDocs(ContentResolver cr, Uri treeUri, String parentDocId,
	                         List<Uri> out, int depth) {
		if (out.size() >= MAX_DOC_FILES || depth > MAX_DOC_DEPTH) return;
		Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId);
		Cursor c = null;
		try {
			c = cr.query(childrenUri, new String[]{
					Document.COLUMN_DOCUMENT_ID,
					Document.COLUMN_DISPLAY_NAME,
					Document.COLUMN_MIME_TYPE
			}, null, null, null);
			if (c == null) return;
			while (c.moveToNext() && out.size() < MAX_DOC_FILES) {
				String docId = c.getString(0);
				String name = c.getString(1);
				String mime = c.getString(2);
				if (mime != null && mime.equals(Document.MIME_TYPE_DIR)) {
					collectDocs(cr, treeUri, docId, out, depth + 1);
				} else if (name != null) {
					String up = name.toUpperCase();
					if (up.endsWith(".XML") || up.endsWith(".MXL") || up.endsWith(".MUSICXML")) {
						out.add(DocumentsContract.buildDocumentUriUsingTree(treeUri, docId));
					}
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			if (c != null) try { c.close(); } catch (Throwable ignored) {}
		}
	}

	private void refreshDocList() {
		items = new ArrayList<>();
		paths = new ArrayList<>();
		if (docUris == null || docUris.isEmpty()) {
			items.add(getContext().getString(R.string.browse_doc_empty));
			paths.add("");
			setAdapter(new ArrayAdapter<>(getContext(),
					android.R.layout.simple_list_item_1, items));
			return;
		}
		for (Uri u : docUris) {
			String name = u.getLastPathSegment();
			if (name != null) {
				int slash = name.lastIndexOf('/');
				if (slash >= 0) name = name.substring(slash + 1);
			}
			items.add(name == null ? "unknown" : name);
			paths.add(u.toString());
		}
		setAdapter(new ArrayAdapter<>(getContext(),
				android.R.layout.simple_list_item_1, items));
	}

	// ==================== 权限/模式切换 ====================

	/**
	 * 根据当前权限状况自动切换浏览模式。
	 * SAF 目录树导入后保持其结果，不被 File 模式覆盖。
	 */
	public void refreshForCurrentAccessMode() {
		if (docMode && docUris != null) {
			refreshDocList();
			return;
		}
		Context ctx = getContext();
		if (ctx instanceof PianoShelfActivity) {
			PianoShelfActivity a = (PianoShelfActivity) ctx;
			if (a.isExternalStorageManager()) {
				this.rootMode = true;
				this.rootpath = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
				getFileDir(this.rootpath);
				return;
			}
		}
		if (!this.rootMode) {
			this.rootpath = "/";
			getFileDir(this.rootpath);
		}
	}

	// ==================== File 模式 ====================

	public void getFileDir(String filePath) {
		try {
			items = new ArrayList<>();
			paths = new ArrayList<>();
			File f = new File(filePath);
			File[] files = f.listFiles(new MusicFileFilter());

			if (!filePath.equals(rootpath)) {
				items.add(this.getContext().getString(R.string.dis_back_root));
				paths.add(rootpath);
				items.add(String.format(this.getContext().getString(R.string.dis_back_up),
						f.getParent() == null ? rootpath : f.getParent()));
				paths.add(f.getParent() == null ? rootpath : f.getParent());
			}
			int dataCount = 0;
			if (files != null) {
				for (File file : files) {
					String itemsname = file.getName();
					if (file.isDirectory()) itemsname = "[" + itemsname + "]";
					items.add(itemsname);
					paths.add(file.getPath());
					dataCount++;
				}
			}
			if (dataCount == 0) {
				items.add(this.getContext().getString(R.string.browse_empty_hint));
				paths.add("");
			}
			ArrayAdapter<String> fileList = new ArrayAdapter<>(this.getContext(),
					android.R.layout.simple_list_item_1, items);
			setAdapter(fileList);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ==================== 点击/长按 ====================

	@Override
	public void onItemClick(AdapterView<?> l, View v, int position, long id) {
		int dataPos = position - getHeaderViewsCount();
		if (dataPos < 0 || dataPos >= paths.size()) return;
		String pathOrUri = paths.get(dataPos);
		if (pathOrUri == null || pathOrUri.isEmpty()) return; // 提示行，不可点

		if (docMode) {
			openDocUri(Uri.parse(pathOrUri), true);
			return;
		}
		File file = new File(pathOrUri);
		if (!file.canRead()) {
			Toast.makeText(getContext(), getContext().getString(R.string.info_canot_read),
					Toast.LENGTH_SHORT).show();
			return;
		}
		if (file.isDirectory()) {
			getFileDir(pathOrUri);
		} else if (file.isFile()) {
			dbhelp.insertRecentItem(file.getPath());
			launchGraphicsActivity(getContext(), file);
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		int dataPos = position - getHeaderViewsCount();
		if (dataPos < 0 || dataPos >= paths.size()) return true;
		String pathOrUri = paths.get(dataPos);
		if (pathOrUri == null || pathOrUri.isEmpty()) return true;

		if (docMode) {
			openDocUri(Uri.parse(pathOrUri), false);
			return true;
		}
		File file = new File(pathOrUri);
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

	/**
	 * 打开 SAF 文件：后台 materialize 到 cache，再启动 GraphicsActivity。
	 * @param addToRecent true=点击打开(记最近); false=长按(记收藏)
	 */
	private void openDocUri(Uri uri, boolean addToRecent) {
		final Context ctx = getContext();
		new Thread(() -> {
			String path = SafeFileResolver.materializeToCacheFile(ctx, uri);
			runOnUi(() -> {
				if (path == null) {
					Toast.makeText(ctx, ctx.getString(R.string.info_open_err),
							Toast.LENGTH_LONG).show();
					return;
				}
				if (dbhelp != null) {
					if (addToRecent) dbhelp.insertRecentItem(path);
					else {
						dbhelp.insertFavoriteItem(path);
						Toast.makeText(ctx, ctx.getString(R.string.info_add_favorite),
								Toast.LENGTH_SHORT).show();
					}
				}
				if (addToRecent) {
					launchGraphicsActivity(ctx, new File(path));
				}
			});
		}, "doc-open").start();
	}

	private void runOnUi(Runnable r) {
		Context ctx = getContext();
		if (ctx instanceof Activity) ((Activity) ctx).runOnUiThread(r);
		else new Handler(Looper.getMainLooper()).post(r);
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
			// 直接传文件路径，GraphicsActivity 优先读这个 extra，
			// 避免 content:// URI → new File(new URI(...)) 的 IllegalArgumentException
			intent.putExtra(GraphicsActivity.EXTRA_FILE_PATH, file.getAbsolutePath());
			context.startActivity(intent);
		} catch (Throwable t) {
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
			return upper.endsWith(".XML") || upper.endsWith(".MXL") || upper.endsWith(".MUSICXML");
		}
	}
}
