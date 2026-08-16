package com.rojama.pianoshelf;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import androidx.core.content.FileProvider;

/**
 * Tab 1: 文件浏览器 — 支持根目录浏览版。
 *
 * 访问模式（优先级从高到低）:
 *   1. ROOT 模式：用户授予了 MANAGE_EXTERNAL_STORAGE → 直接用 File API 访问
 *      /storage/emulated/0 整个外部存储，像 ES File Explorer 那样
 *   2. SAF 模式：用户通过 ACTION_OPEN_DOCUMENT_TREE 授权了某个目录
 *   3. Legacy 模式：Android 6-12 且有 READ_EXTERNAL_STORAGE 权限
 *   4. MediaStore 模式：Android 13+ 默认的零权限兜底
 */
public class TabBrowseList extends ListView implements OnItemClickListener, OnItemLongClickListener {
	private static final String FILEPROVIDER_AUTH = "com.rojama.pianoshelf.fileprovider";

	// 外部存储根路径
	private static final String EXTERNAL_ROOT = "/storage/emulated/0";

	// 数据源
	private final List<String> displayItems = new ArrayList<>();
	private final List<Entry> displayEntries = new ArrayList<>();

	private static final class Entry {
		enum Kind {
			BACK_TO_ROOT, BACK_TO_PARENT,
			LEGACY_FILE, LEGACY_DIR,
			MEDIASTORE,
			SAF_FILE, SAF_DIR
		}
		final Kind kind;
		final String label;
		final String legacyPath;
		final PianoShelfActivity.MediaStoreMusicXmlEntry media;
		final SafeFileResolver.DocEntry doc;
		final android.net.Uri safTreeUri;

		Entry(Kind k, String label, String legacyPath,
				PianoShelfActivity.MediaStoreMusicXmlEntry media,
				SafeFileResolver.DocEntry doc, android.net.Uri safTreeUri) {
			this.kind = k; this.label = label; this.legacyPath = legacyPath;
			this.media = media; this.doc = doc; this.safTreeUri = safTreeUri;
		}
	}

	DatabaseHelper dbhelp;
	private String rootpath = "/";
	private String currentLegacyPath = "/";

	// Root browsing mode state
	private boolean rootBrowsingMode = false;
	private String currentRootPath = EXTERNAL_ROOT;

	// SAF 目录树模式
	private android.net.Uri boundTreeUri = null;
	private String currentSafDocId = null;

	// Header views
	private View headerView = null;
	private Button btnBrowseRoot = null;
	private Button btnRootAccess = null;
	private TextView permHintView = null;

	public TabBrowseList(Context context) {
		super(context);
		this.setOnItemClickListener(this);
		this.setOnItemLongClickListener(this);
		dbhelp = ((PianoShelfActivity) context).dbhelp;

		inflateAndAttachHeader();
		refreshForCurrentAccessMode();
	}

	// ------------------------------------------------------------------
	// Header: SAF 入口按钮 + 根目录浏览入口
	// ------------------------------------------------------------------

	private void inflateAndAttachHeader() {
		if (headerView != null) return;
		headerView = buildHeaderProgrammatically();
		try {
			addHeaderView(headerView, null, false);
		} catch (Throwable ignored) {}
	}

	private View buildHeaderProgrammatically() {
		final Context ctx = getContext();
		LinearLayout root = new LinearLayout(ctx);
		root.setOrientation(LinearLayout.VERTICAL);
		int pad = dp(ctx, 12);
		root.setPadding(pad, pad, pad, 0);

		TextView hint = new TextView(ctx);
		hint.setText(R.string.browse_tab_header_hint);
		hint.setTextSize(13);
		hint.setPadding(0, 0, 0, pad / 2);
		root.addView(hint);

		// 第一行：打开文件 + 选择目录
		LinearLayout btnRow1 = new LinearLayout(ctx);
		btnRow1.setOrientation(LinearLayout.HORIZONTAL);
		btnRow1.setPadding(0, 0, 0, pad / 2);
		btnRow1.setWeightSum(2);

		Button btnFile = new Button(ctx);
		btnFile.setText(R.string.browse_btn_open_file);
		btnFile.setOnClickListener(new OnClickListener() {
			@Override public void onClick(View v) {
				if (ctx instanceof PianoShelfActivity) {
					((PianoShelfActivity) ctx).openSystemFilePickerForMusicXml(
							PianoShelfActivity.REQ_OPEN_MUSICXML_FILE);
				}
			}
		});
		LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		p1.setMarginEnd(pad / 2);
		btnRow1.addView(btnFile, p1);

		Button btnDir = new Button(ctx);
		btnDir.setText(R.string.browse_btn_open_dir);
		btnDir.setOnClickListener(new OnClickListener() {
			@Override public void onClick(View v) {
				if (ctx instanceof PianoShelfActivity) {
					((PianoShelfActivity) ctx).openSystemFilePickerForMusicXml(
							PianoShelfActivity.REQ_OPEN_MUSICXML_TREE);
				}
			}
		});
		LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(
				0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
		p2.setMarginStart(pad / 2);
		btnRow1.addView(btnDir, p2);
		root.addView(btnRow1);

		// 第二行：浏览根目录（有权限直接进，无权限去授权）
		LinearLayout btnRow2 = new LinearLayout(ctx);
		btnRow2.setOrientation(LinearLayout.HORIZONTAL);
		btnRow2.setPadding(0, 0, 0, pad / 2);

		btnBrowseRoot = new Button(ctx);
		btnBrowseRoot.setText(R.string.browse_btn_browse_root);
		btnBrowseRoot.setOnClickListener(new OnClickListener() {
			@Override public void onClick(View v) {
				openRootDirectory();
			}
		});
		LinearLayout.LayoutParams p3 = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		btnRow2.addView(btnBrowseRoot, p3);
		root.addView(btnRow2);

		// 权限提示（未授权时显示）
		permHintView = new TextView(ctx);
		permHintView.setText(R.string.browse_root_permission_required);
		permHintView.setTextSize(12);
		permHintView.setPadding(0, 0, 0, pad / 2);
		root.addView(permHintView);

		// 授权按钮
		btnRootAccess = new Button(ctx);
		btnRootAccess.setText(R.string.browse_root_go_settings);
		btnRootAccess.setOnClickListener(new OnClickListener() {
			@Override public void onClick(View v) {
				if (ctx instanceof PianoShelfActivity) {
					((PianoShelfActivity) ctx).requestManageExternalStorage();
				}
			}
		});
		LinearLayout.LayoutParams p4 = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		p4.setPadding(0, 0, 0, pad / 2);
		root.addView(btnRootAccess, p4);

		TextView note = new TextView(ctx);
		note.setText(R.string.browse_scoped_storage_note);
		note.setTextSize(12);
		note.setPadding(0, 0, 0, pad);
		root.addView(note);

		View sep = new View(ctx);
		sep.setBackgroundColor(0x33808080);
		LinearLayout.LayoutParams ps = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, 1);
		ps.bottomMargin = pad;
		root.addView(sep, ps);

		return root;
	}

	private static int dp(Context ctx, int dip) {
		float d = ctx.getResources().getDisplayMetrics().density;
		return (int) (dip * d + 0.5f);
	}

	// ------------------------------------------------------------------
	// 对外刷新入口
	// ------------------------------------------------------------------

	public void refreshForCurrentAccessMode() {
		PianoShelfActivity act = (PianoShelfActivity) getContext();
		boolean hasRootPerm = act.isExternalStorageManager();

		// 更新权限提示可见性
		updatePermissionUI(hasRootPerm);

		if (rootBrowsingMode) {
			// 已在根浏览模式，保持显示
			getFileDir(currentRootPath);
		} else if (hasRootPerm) {
			// 有权限：直接进入根浏览模式，列出 /storage/emulated/0
			rootBrowsingMode = true;
			currentRootPath = EXTERNAL_ROOT;
			getFileDir(EXTERNAL_ROOT);
		} else if (boundTreeUri != null) {
			loadSafTree(currentSafDocId);
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			loadMediaStoreFiles();
		} else {
			try {
				int r = androidx.core.content.ContextCompat.checkSelfPermission(
						getContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE);
				if (r == android.content.pm.PackageManager.PERMISSION_GRANTED) {
					getFileDir(currentLegacyPath);
				} else {
					showInitialContent();
				}
			} catch (Throwable ignore) {
				showInitialContent();
			}
		}
	}

	private void updatePermissionUI(boolean hasRootPerm) {
		int visibility = hasRootPerm ? View.GONE : View.VISIBLE;
		if (permHintView != null) {
			permHintView.setVisibility(visibility);
		}
		if (btnRootAccess != null) {
			btnRootAccess.setVisibility(visibility);
		}
		if (btnBrowseRoot != null) {
			btnBrowseRoot.setText(hasRootPerm
					? R.string.browse_btn_continue_root
					: R.string.browse_btn_browse_root);
		}
	}

	private void showInitialContent() {
		// 显示 MediaStore 文件列表 + 提示
		loadMediaStoreFiles();
	}

	/** 打开根目录浏览 */
	public void openRootDirectory() {
		PianoShelfActivity act = (PianoShelfActivity) getContext();
		if (!act.isExternalStorageManager()) {
			act.requestManageExternalStorage();
			return;
		}
		// 已有权限：进入根浏览模式
		rootBrowsingMode = true;
		currentRootPath = EXTERNAL_ROOT;
		Toast.makeText(getContext(), R.string.browse_root_toast_listing, Toast.LENGTH_SHORT).show();
		getFileDir(EXTERNAL_ROOT);
	}

	/** 退出根浏览模式 */
	public void exitRootBrowsing() {
		rootBrowsingMode = false;
		boundTreeUri = null;
		// 不自动重新进入根浏览，改为展示 MediaStore 文件列表
		loadMediaStoreFiles();
	}

	/** 用户选过 ACTION_OPEN_DOCUMENT_TREE 后绑定 */
	public void bindToDocumentTree(android.net.Uri treeUri) {
		this.boundTreeUri = treeUri;
		this.currentSafDocId = SafeFileResolver.getRootDocId(treeUri);
		this.rootBrowsingMode = false;
		refreshForCurrentAccessMode();
	}

	// ------------------------------------------------------------------
	// 模式 A：Legacy / Root 目录树
	// ------------------------------------------------------------------

	public void getFileDir() {
		getFileDir(currentLegacyPath);
	}

	@SuppressWarnings("deprecation")
	public void getFileDir(String filePath) {
		try {
			displayItems.clear();
			displayEntries.clear();
			File f = new File(filePath);
			if (!f.exists()) {
				loadMediaStoreFiles();
				return;
			}
			if (!f.canRead()) {
				// 尝试用 root 权限
				PianoShelfActivity act = (PianoShelfActivity) getContext();
				if (act.isExternalStorageManager()) {
					// 有 root 权限但仍读不到，可能是路径问题
					Toast.makeText(getContext(), R.string.browse_root_failed, Toast.LENGTH_LONG).show();
					rootBrowsingMode = false;
					loadMediaStoreFiles();
					return;
				} else {
					Toast.makeText(getContext(), R.string.browse_root_failed, Toast.LENGTH_LONG).show();
					rootBrowsingMode = false;
					loadMediaStoreFiles();
					return;
				}
			}
			File[] files = f.listFiles(new MusicFileFilter());

			// 添加导航条目
			if (!filePath.equals(rootpath) && !filePath.equals(EXTERNAL_ROOT)) {
				displayItems.add(getContext().getString(R.string.dis_back_root));
				displayEntries.add(new Entry(Entry.Kind.BACK_TO_ROOT, null, rootpath, null, null, null));
				String parent = f.getParent();
				if (parent != null && !parent.equals("/")) {
					displayItems.add(String.format(getContext().getString(R.string.dis_back_up), parent));
					displayEntries.add(new Entry(Entry.Kind.BACK_TO_PARENT, null, parent, null, null, null));
				} else {
					// 到达根目录的特殊处理
					displayItems.add(String.format(getContext().getString(R.string.dis_back_up), EXTERNAL_ROOT));
					displayEntries.add(new Entry(Entry.Kind.BACK_TO_PARENT, null, EXTERNAL_ROOT, null, null, null));
				}
			} else if (filePath.equals(EXTERNAL_ROOT) && !rootBrowsingMode) {
				// 非 root 浏览模式下在 /storage/emulated/0 根
				displayItems.add(getContext().getString(R.string.dis_back_root));
				displayEntries.add(new Entry(Entry.Kind.BACK_TO_ROOT, null, rootpath, null, null, null));
			} else if (rootBrowsingMode && filePath.equals(EXTERNAL_ROOT)) {
				// 在 root 浏览模式下显示"退出根浏览"选项
				displayItems.add("[退出根浏览]");
				displayEntries.add(new Entry(Entry.Kind.BACK_TO_ROOT, null, "__EXIT_ROOT__", null, null, null));
			}

			if (files != null) {
				for (File file : files) {
					String name = file.getName();
					String label = file.isDirectory() ? "[" + name + "]" : name;
					displayItems.add(label);
					displayEntries.add(new Entry(
							file.isDirectory() ? Entry.Kind.LEGACY_DIR : Entry.Kind.LEGACY_FILE,
							label, file.getAbsolutePath(), null, null, null));
				}
			}
			if (rootBrowsingMode) {
				currentRootPath = filePath;
			} else {
				currentLegacyPath = filePath;
			}
			applyAdapter();
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(getContext(), R.string.info_open_err, Toast.LENGTH_SHORT).show();
			if (rootBrowsingMode) rootBrowsingMode = false;
			loadMediaStoreFiles();
		}
	}

	// ------------------------------------------------------------------
	// 模式 B：MediaStore 零权限聚合
	// ------------------------------------------------------------------

	private void loadMediaStoreFiles() {
		List<PianoShelfActivity.MediaStoreMusicXmlEntry> list =
				PianoShelfActivity.queryAllMusicXmlViaMediaStore(getContext());
		displayItems.clear();
		displayEntries.clear();
		if (list == null || list.isEmpty()) {
			displayItems.add(getContext().getString(R.string.browse_no_matching_files));
			displayEntries.add(null);
		} else {
			for (PianoShelfActivity.MediaStoreMusicXmlEntry e : list) {
				displayItems.add(e.displayName);
				displayEntries.add(new Entry(Entry.Kind.MEDIASTORE, e.displayName,
						e.absolutePath, e, null, null));
			}
		}
		applyAdapter();
	}

	// ------------------------------------------------------------------
	// 模式 C：SAF 目录树
	// ------------------------------------------------------------------

	private void loadSafTree(String docId) {
		if (boundTreeUri == null) return;
		ArrayList<SafeFileResolver.DocEntry> children =
				SafeFileResolver.listTreeChildren(getContext(), boundTreeUri, docId);
		displayItems.clear();
		displayEntries.clear();
		String rootDocId = SafeFileResolver.getRootDocId(boundTreeUri);
		if (docId == null) docId = rootDocId;
		if (!(docId == rootDocId || (docId != null && docId.equals(rootDocId)))) {
			displayItems.add(getContext().getString(R.string.dis_back_up, "…"));
			displayEntries.add(new Entry(Entry.Kind.BACK_TO_ROOT, null, null, null, null, null));
		}
		if (children == null || children.isEmpty()) {
			displayItems.add(getContext().getString(R.string.browse_no_matching_files));
			displayEntries.add(null);
		} else {
			for (SafeFileResolver.DocEntry d : children) {
				String label = d.isDir ? "[" + d.displayName + "]" : d.displayName;
				displayItems.add(label);
				displayEntries.add(new Entry(
						d.isDir ? Entry.Kind.SAF_DIR : Entry.Kind.SAF_FILE,
						label, null, null, d, boundTreeUri));
			}
		}
		currentSafDocId = docId;
		applyAdapter();
	}

	// ------------------------------------------------------------------
	// Adapter
	// ------------------------------------------------------------------

	private ArrayAdapter<String> adapter = null;

	private void applyAdapter() {
		if (adapter == null) {
			adapter = new ArrayAdapter<>(getContext(),
					android.R.layout.simple_list_item_1, displayItems);
			setAdapter(adapter);
		} else {
			adapter.notifyDataSetChanged();
		}
	}

	// ------------------------------------------------------------------
	// 点击 & 长按
	// ------------------------------------------------------------------

	@Override
	public void onItemClick(AdapterView<?> l, View v, int position, long id) {
		int headerCount = getHeaderViewsCount();
		int pos = position - headerCount;
		if (pos < 0 || pos >= displayEntries.size()) return;
		Entry e = displayEntries.get(pos);
		if (e == null) return;

		switch (e.kind) {
			case BACK_TO_ROOT:
				if ("__EXIT_ROOT__".equals(e.legacyPath)) {
					// 退出根浏览模式
					exitRootBrowsing();
				} else if (boundTreeUri != null) {
					loadSafTree(SafeFileResolver.getRootDocId(boundTreeUri));
				} else if (rootBrowsingMode) {
					getFileDir(EXTERNAL_ROOT);
				} else {
					getFileDir(rootpath);
				}
				return;
			case BACK_TO_PARENT:
				getFileDir(e.legacyPath);
				return;

			case LEGACY_DIR:
				getFileDir(e.legacyPath);
				return;
			case LEGACY_FILE: {
				File file = new File(e.legacyPath);
				if (!file.canRead()) {
					toast(R.string.info_canot_read);
					return;
				}
				dbhelp.insertRecentItem(file.getPath());
				launchGraphicsActivity(getContext(), file);
				return;
			}

			case MEDIASTORE: {
				PianoShelfActivity.MediaStoreMusicXmlEntry m = e.media;
				String path = tryOpenMediaStoreEntry(m);
				if (path == null) {
					toast(R.string.info_open_err);
					return;
				}
				dbhelp.insertRecentItem(path);
				launchGraphicsActivity(getContext(), new File(path));
				return;
			}

			case SAF_DIR: {
				loadSafTree(e.doc.docId);
				return;
			}
			case SAF_FILE: {
				android.net.Uri docUri = SafeFileResolver.buildChildUri(e.safTreeUri, e.doc.docId);
				if (docUri == null) { toast(R.string.info_open_err); return; }
				String path = SafeFileResolver.materializeToCacheFile(getContext(), docUri);
				if (path == null) { toast(R.string.info_open_err); return; }
				dbhelp.insertRecentItem(path);
				launchGraphicsActivity(getContext(), new File(path));
				return;
			}
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		int headerCount = getHeaderViewsCount();
		int pos = position - headerCount;
		if (pos < 0 || pos >= displayEntries.size()) return true;
		Entry e = displayEntries.get(pos);
		if (e == null) return true;

		String fileAbsPath = null;
		switch (e.kind) {
			case LEGACY_FILE:
				fileAbsPath = e.legacyPath;
				break;
			case MEDIASTORE:
				fileAbsPath = tryOpenMediaStoreEntry(e.media);
				break;
			case SAF_FILE: {
				android.net.Uri docUri = SafeFileResolver.buildChildUri(e.safTreeUri, e.doc.docId);
				if (docUri != null) fileAbsPath = SafeFileResolver.materializeToCacheFile(
						getContext(), docUri);
				break;
			}
			default:
				toast(R.string.info_canot_read);
				return true;
		}
		if (fileAbsPath == null || !new File(fileAbsPath).canRead()) {
			toast(R.string.info_canot_read);
			return true;
		}
		dbhelp.insertFavoriteItem(fileAbsPath);
		toast(R.string.info_add_favorite);
		return true;
	}

	private void toast(int resId) {
		Toast.makeText(getContext(), getContext().getString(resId), Toast.LENGTH_SHORT).show();
	}

	private String tryOpenMediaStoreEntry(PianoShelfActivity.MediaStoreMusicXmlEntry m) {
		if (m == null) return null;
		if (m.absolutePath != null && !m.absolutePath.isEmpty()) {
			File f = new File(m.absolutePath);
			if (f.isFile() && f.canRead()) return f.getAbsolutePath();
		}
		try {
			android.net.Uri uri = android.content.ContentUris.withAppendedId(
					android.provider.MediaStore.Files.getContentUri(
							Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
									? android.provider.MediaStore.VOLUME_EXTERNAL : "external"),
					m.id);
			return SafeFileResolver.materializeToCacheFile(getContext(), uri);
		} catch (Throwable ignore) {
			return null;
		}
	}

	// ------------------------------------------------------------------
	// 辅助
	// ------------------------------------------------------------------

	static void launchGraphicsActivity(Context context, File file) {
		try {
			android.net.Uri uri = FileProvider.getUriForFile(context, FILEPROVIDER_AUTH, file);
			Intent intent = new Intent();
			intent.setDataAndType(uri, "application/vnd.recordare.musicxml");
			intent.setClass(context, GraphicsActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
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
			if (name.startsWith(".") || "LOST.DIR".equals(name)) {
				return false;
			}
			if (file.isDirectory()) return true;
			String upper = name.toUpperCase();
			return upper.endsWith(".XML") || upper.endsWith(".MXL") || upper.endsWith(".MUSICXML");
		}
	}
}
