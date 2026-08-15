package com.rojama.pianoshelf;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 主界面 (入口 Activity) — Scoped Storage 重构版。
 *
 * 背景（为什么小米 14 / 安卓 14 浏览乐谱是空的）：
 *   旧版 PianoShelfActivity 通过 TabBrowseList 直接递归扫描
 *   Environment.getExternalStorageDirectory()（= /sdcard 根），依赖
 *   READ_EXTERNAL_STORAGE。但从 Android 13 (API 33, 小米14 出厂/常搭载) 开始，
 *   该权限变成「零授予」：就算 AndroidManifest 声明了、用户也点了允许，
 *   ContextCompat.checkSelfPermission 依然是 DENIED，于是 File("/").listFiles()
 *   一律返回 null → TabBrowseList 看见的就是空的。
 *
 * 修复策略（按稳定性 / 兼容性排序）：
 *   [A] 对 Android 11+（API 30+，即 Scoped Storage 强制打开）：
 *        - TabBrowse 顶部新增两块入口：
 *            ① 「打开乐谱文件」→ ACTION_OPEN_DOCUMENT 系统文件选择器
 *                (用户可在任意路径/USB/云盘上挑选 .xml / .mxl / .musicxml)
 *            ② 「选择目录」→ ACTION_OPEN_DOCUMENT_TREE (用户授权一个目录后，
 *                通过 DocumentsContract 列出该目录下所有乐谱)
 *        - 同时在 TabBrowseList 的空白 header 处额外显示
 *            MediaStore.Files 聚合查询到的所有 .xml/.mxl（系统媒体索引，
 *            无需任何存储权限即可读取），这是"零配置"能看到的最多文件。
 *   [B] 对 Android 6 ~ 12（API 23-32）：保留旧的动态权限 + 文件目录树，
 *        这部分逻辑已经可用，不做破坏性更改。
 *   [C] GraphicsActivity 接收 content:// URI（来自 SAF/FileProvider/外部 App）
 *        后统一复制到 app cache，再走原 FileReader 路径，避免原始
 *        "new File(new URI(data))" 解析 content:// 时直接抛 IllegalArgumentException。
 *   [D] 权限请求时机：只在实际需要 legacy tree 时才请求旧 READ 权限；Android13+
 *        一律引导用户走 SAF，不再弹"被拒的假权限"骚扰用户。
 */
public class PianoShelfActivity extends AppCompatActivity {
	private static final String TAG = "PianoShelf";

	// 权限 / SAF request codes
	private static final int REQ_LEGACY_STORAGE = 1000;
	public  static final int REQ_OPEN_MUSICXML_FILE = 1010;
	public  static final int REQ_OPEN_MUSICXML_TREE = 1011;

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
			mTabHost.addTab(mTabHost.newTabSpec("online").setContent(R.id.tab_online)
					.setIndicator(getText(R.string.tab_online)));

			LinearLayout ll = (LinearLayout) this.findViewById(R.id.tab_browse);
			tbl = new TabBrowseList(this);
			ll.addView(tbl, new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
			ll = (LinearLayout) this.findViewById(R.id.tab_recent);
			trl = new TabRecentList(this);
			ll.addView(trl);
			ll = (LinearLayout) this.findViewById(R.id.tab_favorite);
			tfl = new TabFavoriteList(this);
			ll.addView(tfl);
			ll = (LinearLayout) this.findViewById(R.id.tab_online);
			ll.addView(new TabOnlineWelcome(this));

			mTabHost.setOnTabChangedListener(new OnTabChangeListener() {
				@Override
				public void onTabChanged(String tabId) {
					if ("browse".equals(tabId)) {
						// 切回浏览 Tab 时尝试刷新（Android13+ 可能刚授权了目录）
						if (tbl != null) tbl.refreshForCurrentAccessMode();
					} else if ("recent".equals(tabId)) {
						if (trl != null) trl.getFileDir();
					} else if ("favorite".equals(tabId)) {
						if (tfl != null) tfl.getFileDir();
					}
				}
			});

			// 后台加载音符资源（耗时）
			LoadThread load = new LoadThread();
			load.context = this;
			load.start();

			// Android 6-12: 申请 legacy READ/WRITE（用于 TabBrowse 目录树）
			// Android 13+: 不再申请无效权限，直接走 SAF。
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
					&& Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
				requestLegacyStoragePermissionIfNeeded();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 被 TabBrowseList 调起：打开系统 SAF 文件选择器。
	 */
	public void openSystemFilePickerForMusicXml(int requestCode) {
		try {
			Intent intent;
			if (requestCode == REQ_OPEN_MUSICXML_TREE) {
				if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
					Toast.makeText(this, R.string.info_open_err, Toast.LENGTH_SHORT).show();
					return;
				}
				intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
				intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
						| Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
						| Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
			} else {
				intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				intent.setType("*/*");
				String[] mime = {
						"application/vnd.recordare.musicxml",
						"application/vnd.recordare.musicxml+xml",
						"text/xml",
						"application/xml",
						"application/zip",
						"application/octet-stream"
				};
				intent.putExtra(Intent.EXTRA_MIME_TYPES, mime);
			}
			startActivityForResult(intent, requestCode);
		} catch (Throwable t) {
			Toast.makeText(this, getString(R.string.info_open_err) + " " + t.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) return;
		Uri uri = data.getData();
		try {
			if (requestCode == REQ_OPEN_MUSICXML_TREE
					&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				// 持久化该目录授权，下次进入 app 还能继续访问
				int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION
						| Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
				try {
					getContentResolver().takePersistableUriPermission(uri, takeFlags);
				} catch (Throwable ignored) { /* 某些 ROM 不支持 persistable */ }
				if (tbl != null) tbl.bindToDocumentTree(uri);
				return;
			}
			if (requestCode == REQ_OPEN_MUSICXML_FILE) {
				// 单文件：立即转成本地缓存 File 并打开
				String openedPath = SafeFileResolver.materializeToCacheFile(this, uri);
				if (openedPath == null) {
					Toast.makeText(this, R.string.info_open_err, Toast.LENGTH_LONG).show();
					return;
				}
				if (dbhelp != null) dbhelp.insertRecentItem(openedPath);
				TabBrowseList.launchGraphicsActivity(this, new File(openedPath));
			}
		} catch (Throwable t) {
			Toast.makeText(this, getString(R.string.info_open_err) + " " + t.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}

	// -------------------------------------------------------------------
	// Legacy permission flow (Android 6-12 only)
	// -------------------------------------------------------------------

	private void requestLegacyStoragePermissionIfNeeded() {
		int r = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
		if (r != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE,
					               Manifest.permission.READ_EXTERNAL_STORAGE },
					REQ_LEGACY_STORAGE);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == REQ_LEGACY_STORAGE && grantResults.length > 0
				&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			if (tbl != null) tbl.refreshForCurrentAccessMode();
			if (trl != null) trl.getFileDir();
			if (tfl != null) tfl.getFileDir();
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
		menu.add(group1, 1, 1, getString(R.string.menu_online_score));
		menu.add(group1, 2, 2, getString(R.string.menu_setting));
		menu.add(group1, 3, 3, getString(R.string.menu_exit));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case 1:
				startActivity(new Intent(this, OnlineScoreActivity.class));
				break;
			case 2:
				Intent intent = new Intent();
				intent.setClass(this, AppPreferenceActivity.class);
				startActivity(intent);
				break;
			case 3:
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

	// -------------------------------------------------------------------
	// Scoped Storage 辅助小工具（静态内聚在入口 Activity，便于全局重用）
	// -------------------------------------------------------------------

	/**
	 * Android 11+ 下可零权限从 MediaStore.Files 表拉所有 .xml/.mxl 条目，
	 * 作为「浏览乐谱」Tab 的 header 提示列表 / 初始内容填充。
	 *
	 * 说明：MediaStore 索引的是 "所有 app 写入过的公共媒体文件"，所以只要
	 * 用户曾经用微信/QQ/浏览器/系统文件管理器把 .xml 保存到
	 * Download/Documents/Music/...，这里都会被收录。
	 */
	public static final class MediaStoreMusicXmlEntry {
		public final String displayName;
		public final String absolutePath;   // _data；可能在 Scoped Storage 下为空或不可访问（此时用 openInputStream）
		public final long   id;             // MediaStore row id
		public final long   size;
		public MediaStoreMusicXmlEntry(String displayName, String absolutePath, long id, long size) {
			this.displayName = displayName; this.absolutePath = absolutePath;
			this.id = id; this.size = size;
		}
	}

	public static List<MediaStoreMusicXmlEntry> queryAllMusicXmlViaMediaStore(@NonNull Context ctx) {
		List<MediaStoreMusicXmlEntry> out = new ArrayList<>();
		try {
			ContentResolver cr = ctx.getContentResolver();
			Uri coll;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
				coll = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL);
			} else {
				coll = MediaStore.Files.getContentUri("external");
			}
			String[] proj = {
					MediaStore.Files.FileColumns._ID,
					MediaStore.Files.FileColumns.DISPLAY_NAME,
					Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ? MediaStore.Files.FileColumns.DATA
							: MediaStore.Files.FileColumns.RELATIVE_PATH,
					MediaStore.Files.FileColumns.SIZE,
					MediaStore.Files.FileColumns.MIME_TYPE
			};
			// 用 LIKE 过滤扩展名比 MIME 更稳，因为很多浏览器下载下来的 .xml 并没有正确 mime
			String sel = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
					? MediaStore.Files.FileColumns.DISPLAY_NAME + " LIKE ? OR "
					+ MediaStore.Files.FileColumns.DISPLAY_NAME + " LIKE ? OR "
					+ MediaStore.Files.FileColumns.DISPLAY_NAME + " LIKE ?"
					: MediaStore.Files.FileColumns.DISPLAY_NAME + " LIKE ? OR "
					+ MediaStore.Files.FileColumns.DISPLAY_NAME + " LIKE ? OR "
					+ MediaStore.Files.FileColumns.DISPLAY_NAME + " LIKE ?";
			String[] args = {"%.xml", "%.mxl", "%.musicxml"};
			Cursor c = cr.query(coll, proj, sel, args,
					MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC");
			if (c == null) return out;
			try {
				int ciId = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
				int ciName = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
				int ciPath = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
						? c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
						: c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.RELATIVE_PATH);
				int ciSize = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
				while (c.moveToNext()) {
					String name = c.getString(ciName);
					if (TextUtils.isEmpty(name)) continue;
					String low = name.toLowerCase();
					if (!(low.endsWith(".xml") || low.endsWith(".mxl") || low.endsWith(".musicxml"))) continue;
					String pathOrRel = c.isNull(ciPath) ? null : c.getString(ciPath);
					long sz = c.isNull(ciSize) ? 0L : c.getLong(ciSize);
					out.add(new MediaStoreMusicXmlEntry(name, pathOrRel, c.getLong(ciId), sz));
				}
			} finally {
				try { c.close(); } catch (Throwable ignore) {}
			}
			Collections.sort(out, new Comparator<MediaStoreMusicXmlEntry>() {
				@Override public int compare(MediaStoreMusicXmlEntry a, MediaStoreMusicXmlEntry b) {
					return a.displayName.compareToIgnoreCase(b.displayName);
				}
			});
		} catch (Throwable ignore) {
			// 厂商定制 MediaStore 列名异常时直接降级：返回空列表，header 会提示用户用系统选择器
		}
		return out;
	}

	/**
	 * 把任意 content:// / file:// / http(s):// URI 材料化为 app cache 里一份本地可读 File。
	 *
	 * 原 GraphicsActivity 写死为 "new File(new URI(intent.getDataString()))"，这在遇到
	 * content:// 或 URL encoded SAF 路径时一定失败（IllegalArgumentException or
	 * FileNotFoundException），所以我们在两处入口 (SAF & GraphicsActivity) 统一先走
	 * 这里做一次"稳定的本地路径归一化"。
	 *
	 * @return 本地绝对路径；失败返回 null。
	 */
	public static File materializeUriToCacheFileSafe(@NonNull Context ctx, @NonNull Uri src) {
		try {
			String path = SafeFileResolver.materializeToCacheFile(ctx, src);
			return path == null ? null : new File(path);
		} catch (Throwable t) {
			return null;
		}
	}

	/**
	 * @return 当前外部存储根路径（旧版本用）；Scoped Storage 下依旧可返回，
	 *         只是 TabBrowseList 需要意识到这个目录可能 listFiles() 为空。
	 */
	@SuppressWarnings("deprecation")
	public static File getPrimaryExternalRootCompat() {
		try {
			return Environment.getExternalStorageDirectory();
		} catch (Throwable t) {
			return new File("/");
		}
	}
}
