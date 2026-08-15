package com.rojama.pianoshelf;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.LayoutInflater;
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
 * Tab 1: 文件浏览器 — Scoped Storage 重制版。
 *
 * 为什么原代码在小米 14 / Android 14 上是空的：
 *   原实现直接 listFiles("/sdcard") + "向上/向下"目录树。
 *   Android 13+ 上 READ_EXTERNAL_STORAGE 变成「零授予」，
 *   所以 "/" 的 children 一定是 null → 空白屏。
 *
 * 修复：
 *   - 顶部 header 提供两块入口：
 *      ①「打开乐谱文件」→ ACTION_OPEN_DOCUMENT（系统 SAF 文件选择器，可进任意目录/USB/云盘）
 *      ②「选择目录」→ ACTION_OPEN_DOCUMENT_TREE（持久化授权后，列出该目录下所有 .xml/.mxl）
 *   - Android 11+ 默认用 MediaStore.Files 零权限聚合查询填充内容，
 *     展示系统媒体库里所有 .xml/.mxl 文件（用户用微信/浏览器/文件管理器保存过的都会出现）。
 *   - Android 6-12 仍保留 legacy 目录树；用户选过 SAF tree 后优先展示 SAF tree。
 *   - GraphicsActivity 启动统一用 FileProvider (避免 FileUriExposedException)。
 */
public class TabBrowseList extends ListView implements OnItemClickListener, OnItemLongClickListener {
	private static final String FILEPROVIDER_AUTH = "com.rojama.pianoshelf.fileprovider";

	// 数据源
	// displayItems / displayPaths 是 ListView 实际展示内容（含 "返回上一级"、SAF tree 条目、MediaStore 条目等）
	private final List<String> displayItems = new ArrayList<>();
	private final List<Entry> displayEntries = new ArrayList<>();

	private static final class Entry {
		enum Kind {
			BACK_TO_ROOT, BACK_TO_PARENT,   // 导航伪条目
			LEGACY_FILE, LEGACY_DIR,         // 旧版 File API
			MEDIASTORE,                      // MediaStore 查出来的（仅文件）
			SAF_FILE, SAF_DIR                // DocumentsContract 下条目
		}
		final Kind kind;
		final String label;                 // 展示用
		final String legacyPath;            // LEGACY_* 用
		final PianoShelfActivity.MediaStoreMusicXmlEntry media; // MEDIASTORE 用
		final SafeFileResolver.DocEntry doc;  // SAF_* 用
		final Uri safTreeUri;               // SAF_* 用

		Entry(Kind k, String label, String legacyPath,
				PianoShelfActivity.MediaStoreMusicXmlEntry media,
				SafeFileResolver.DocEntry doc, Uri safTreeUri) {
			this.kind = k; this.label = label; this.legacyPath = legacyPath;
			this.media = media; this.doc = doc; this.safTreeUri = safTreeUri;
		}
	}

	DatabaseHelper dbhelp;
	private String rootpath = "/";
	private String currentLegacyPath = "/";

	// SAF 目录树模式：用户选过 ACTION_OPEN_DOCUMENT_TREE 后启用
	private Uri boundTreeUri = null;
	private String currentSafDocId = null;

	// header views (缓存引用，避免重复 add)
	private View headerView = null;

	public TabBrowseList(Context context) {
		super(context);
		this.setOnItemClickListener(this);
		this.setOnItemLongClickListener(this);
		dbhelp = ((PianoShelfActivity) context).dbhelp;

		// 先加 header（SAF 入口 + 提示），再加 adapter
		inflateAndAttachHeader();

		refreshForCurrentAccessMode();
	}

	// ------------------------------------------------------------------
	// Header: SAF 入口按钮 + Scoped Storage 提示
	// ------------------------------------------------------------------

	private void inflateAndAttachHeader() {
		if (headerView != null) return;
		headerView = buildHeaderProgrammatically();
		try {
			addHeaderView(headerView, null, false);
		} catch (Throwable ignored) {
			// 某些旧设备 addHeaderView 需要在 setAdapter 前；已先调用，忽略
		}
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

		LinearLayout btnRow = new LinearLayout(ctx);
		btnRow.setOrientation(LinearLayout.HORIZONTAL);
		btnRow.setPadding(0, 0, 0, pad / 2);
		btnRow.setWeightSum(2);

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
		btnRow.addView(btnFile, p1);

		Button btnDir = new Button(ctx);
		btnDir.setText(R.string.browse_btn_open_dir);
		btnDir.setEnabled(SafeFileResolver.isTreeUriSupported());
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
		btnRow.addView(btnDir, p2);
		root.addView(btnRow);

		TextView note = new TextView(ctx);
		note.setText(R.string.browse_scoped_storage_note);
		note.setTextSize(12);
		note.setPadding(0, 0, 0, pad);
		root.addView(note);

		// 一条分隔线
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
	// 对外刷新入口（PianoShelfActivity 切 Tab / 权限回来 / SAF 返回 时调用）
	// ------------------------------------------------------------------

	public void refreshForCurrentAccessMode() {
		if (boundTreeUri != null) {
			// SAF 目录树模式（用户选过目录，优先展示这个）
			loadSafTree(currentSafDocId);
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			// Android 11+：零权限用 MediaStore 填充（避免空白屏）
			loadMediaStoreFiles();
		} else {
			// Android 6-10：走 legacy 目录树（需要 permission，拿不到时再降级 MediaStore）
			try {
				int r = androidx.core.content.ContextCompat.checkSelfPermission(
						getContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE);
				if (r == android.content.pm.PackageManager.PERMISSION_GRANTED) {
					getFileDir(currentLegacyPath);
				} else {
					loadMediaStoreFiles();
				}
			} catch (Throwable ignore) {
				loadMediaStoreFiles();
			}
		}
	}

	/** 用户选过 ACTION_OPEN_DOCUMENT_TREE 后，PianoShelfActivity 调这里绑定 */
	public void bindToDocumentTree(Uri treeUri) {
		this.boundTreeUri = treeUri;
		this.currentSafDocId = SafeFileResolver.getRootDocId(treeUri);
		refreshForCurrentAccessMode();
	}

	// ------------------------------------------------------------------
	// 模式 A：Legacy 目录树（Android 6-12 且有存储权限时用）
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
			File[] files = f.listFiles(new MusicFileFilter());

			if (!filePath.equals(rootpath)) {
				displayItems.add(getContext().getString(R.string.dis_back_root));
				displayEntries.add(new Entry(Entry.Kind.BACK_TO_ROOT, null, rootpath, null, null, null));
				String parent = f.getParent() == null ? rootpath : f.getParent();
				displayItems.add(String.format(getContext().getString(R.string.dis_back_up), parent));
				displayEntries.add(new Entry(Entry.Kind.BACK_TO_PARENT, null, parent, null, null, null));
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
			currentLegacyPath = filePath;
			applyAdapter();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ------------------------------------------------------------------
	// 模式 B：MediaStore 零权限聚合（Android 11+ 默认 / Legacy 没权限时兜底）
	// ------------------------------------------------------------------

	private void loadMediaStoreFiles() {
		List<PianoShelfActivity.MediaStoreMusicXmlEntry> list =
				PianoShelfActivity.queryAllMusicXmlViaMediaStore(getContext());
		displayItems.clear();
		displayEntries.clear();
		if (list == null || list.isEmpty()) {
			displayItems.add(getContext().getString(R.string.browse_no_matching_files));
			displayEntries.add(null); // 哨兵：点击不响应
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
	// 模式 C：SAF 目录树（用户授权了某个具体目录，支持子目录跳转）
	// ------------------------------------------------------------------

	private void loadSafTree(String docId) {
		if (boundTreeUri == null) return;
		ArrayList<SafeFileResolver.DocEntry> children =
				SafeFileResolver.listTreeChildren(getContext(), boundTreeUri, docId);
		displayItems.clear();
		displayEntries.clear();
		String rootDocId = SafeFileResolver.getRootDocId(boundTreeUri);
		if (docId == null) docId = rootDocId;
		// 非根目录显示"返回"
		if (!(docId == rootDocId || (docId != null && docId.equals(rootDocId)))) {
			displayItems.add(getContext().getString(R.string.dis_back_up, "…"));
			// parent docId 无法直接拿（DocumentsContract 没给简单 API），
			// 简单处理："上一"级退回到根（用户可从根重新进）
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
	// 通用：刷新 adapter
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
		// 减去 header 偏移
		int headerCount = getHeaderViewsCount();
		int pos = position - headerCount;
		if (pos < 0 || pos >= displayEntries.size()) return;
		Entry e = displayEntries.get(pos);
		if (e == null) return; // 哨兵（"未找到文件"）

		switch (e.kind) {
			case BACK_TO_ROOT:
				if (boundTreeUri != null) {
					loadSafTree(SafeFileResolver.getRootDocId(boundTreeUri));
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
				// MediaStore 条目：优先用 _data（老版本），否则 openInputStream → cache
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
				Uri docUri = SafeFileResolver.buildChildUri(e.safTreeUri, e.doc.docId);
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
				Uri docUri = SafeFileResolver.buildChildUri(e.safTreeUri, e.doc.docId);
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

	/**
	 * 打开 MediaStore 条目：
	 *  - 若有绝对 _data 且可读 → 直接返回
	 *  - 否则通过 ContentResolver.openInputStream(_ID) → 拷贝到 cache 返回
	 */
	private String tryOpenMediaStoreEntry(PianoShelfActivity.MediaStoreMusicXmlEntry m) {
		if (m == null) return null;
		// Android 10 以下 DATA 是绝对路径
		if (m.absolutePath != null && !m.absolutePath.isEmpty()) {
			File f = new File(m.absolutePath);
			if (f.isFile() && f.canRead()) return f.getAbsolutePath();
		}
		// Scoped Storage：DATA 可能是 RELATIVE_PATH（只是 "Download/"），不可直接访问
		// 走 ContentUris.withAppendedId(EXTERNAL_CONTENT_URI, id) → 开流 → cache
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
