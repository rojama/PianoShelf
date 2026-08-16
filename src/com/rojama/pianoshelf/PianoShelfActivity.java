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
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 便携乐谱 主界面 (入口 Activity) — HyperOS 3 风格 UI 重写版
 *
 * UI 重构内容（2026-08）：
 *   1. 去除顶部 ActionBar + 三点菜单
 *   2. Tab 从顶部 TabHost 移到屏幕底部（自定义 5 个胶囊按钮）
 *   3. Tab 顺序：浏览 / 最近 / 在线(中间) / 收藏 / 设置
 *   4. 选中 Tab 采用 HyperOS 药丸高亮（靛蓝色胶囊背景）
 *   5. 设置 Tab 独立为最右侧图标，点击直接打开 Settings Activity
 *   6. 退出功能移除（用户按系统返回键即可）
 *
 * 存储访问策略延续上版：
 *   - Android 11+：MANAGE_EXTERNAL_STORAGE 授权 → 直接列 /storage/emulated/0
 *   - 零权限：MediaStore.Files 聚合 + SAF 系统选择器
 */
public class PianoShelfActivity extends AppCompatActivity {
	private static final String TAG = "PianoShelf";

	// Tab position constants (matches bottom nav order in main.xml)
	public static final int TAB_BROWSE   = 0;
	public static final int TAB_RECENT   = 1;
	public static final int TAB_ONLINE   = 2; // CENTER
	public static final int TAB_FAVORITE = 3;
	public static final int TAB_SETTINGS = 4; // RIGHTMOST

	// 权限 / SAF request codes
	private static final int REQ_LEGACY_STORAGE = 1000;
	public  static final int REQ_OPEN_MUSICXML_FILE = 1010;
	public  static final int REQ_OPEN_MUSICXML_TREE = 1011;
	public  static final int REQ_MANAGE_ALL_FILES = 1012;

	// 上半部分内容容器（5 个子容器 FrameLayout 动态加入）
	private FrameLayout mContentFrame;

	// 每个 Tab 对应一个独立容器（wrap TabXXXList），切换用 setVisibility
	private FrameLayout mBrowseContainer;
	private FrameLayout mRecentContainer;
	private FrameLayout mOnlineContainer;
	private FrameLayout mFavoriteContainer;

	// 底部 Tab 按钮（btn = 外层可点击容器；pill = 药丸高亮容器）
	private View[] mTabBtns = new View[5];
	private View[] mTabPills = new View[5];

	private int mCurrentTab = TAB_BROWSE;

	public DatabaseHelper dbhelp;
	TabBrowseList tbl;
	TabRecentList trl;
	TabFavoriteList tfl;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		DebugLog.ensureInitialized(this);
		DebugLog.i(TAG, "========== PianoShelfActivity.onCreate 启动 ==========");
		DebugLog.i(TAG, "Build.VERSION.SDK_INT=" + Build.VERSION.SDK_INT
				+ "  DEVICE=" + Build.DEVICE + "  MODEL=" + Build.MODEL + "  MANUFACTURER=" + Build.MANUFACTURER);

		// --- HyperOS 3 / MIUI 状态栏高度适配 ---
		applyStatusBarInsets();

		try {
			dbhelp = new DatabaseHelper(this);

			// 获取布局中的容器 & Tab 按钮
			mContentFrame = findViewById(R.id.content_frame);

			// Tab 按钮引用（外层 clickable）
			mTabBtns[TAB_BROWSE]   = findViewById(R.id.tab_browse_btn);
			mTabBtns[TAB_RECENT]   = findViewById(R.id.tab_recent_btn);
			mTabBtns[TAB_ONLINE]   = findViewById(R.id.tab_online_btn);
			mTabBtns[TAB_FAVORITE] = findViewById(R.id.tab_favorite_btn);
			mTabBtns[TAB_SETTINGS] = findViewById(R.id.tab_settings_btn);

			// Tab 药丸（承载 bg selector + checked state）
			mTabPills[TAB_BROWSE]   = findViewById(R.id.tab_browse_pill);
			mTabPills[TAB_RECENT]   = findViewById(R.id.tab_recent_pill);
			mTabPills[TAB_ONLINE]   = findViewById(R.id.tab_online_pill);
			mTabPills[TAB_FAVORITE] = findViewById(R.id.tab_favorite_pill);
			mTabPills[TAB_SETTINGS] = findViewById(R.id.tab_settings_pill);

			// ==================== 创建 4 个内容容器（设置 Tab 直接起 Activity） ====================
			mBrowseContainer   = createTabContentWithHeader(R.string.browse_section_subtitle, true);
			mRecentContainer   = createTabContentWithHeader(R.string.recent_section_subtitle, true);
			mOnlineContainer   = createTabContentWithHeader(R.string.online_section_subtitle, true);
			mFavoriteContainer = createTabContentWithHeader(R.string.favorite_section_subtitle, true);

			mContentFrame.addView(mBrowseContainer,   newFrameParams());
			mContentFrame.addView(mRecentContainer,   newFrameParams());
			mContentFrame.addView(mOnlineContainer,   newFrameParams());
			mContentFrame.addView(mFavoriteContainer, newFrameParams());

			// 把原来的 4 个 TabXxxList View 塞进对应的容器（放进 header 之后的垂直 linear 的第 2 格）
			tbl = new TabBrowseList(this);
			addIntoContentContainer(mBrowseContainer, tbl);

			trl = new TabRecentList(this);
			addIntoContentContainer(mRecentContainer, trl);

			tfl = new TabFavoriteList(this);
			addIntoContentContainer(mFavoriteContainer, tfl);

			View onlineWelcome = new TabOnlineWelcome(this);
			addIntoContentContainer(mOnlineContainer, onlineWelcome);

			// ==================== 绑定底部 Tab 点击事件 ====================
			for (int i = 0; i < 5; i++) {
				final int pos = i;
				if (mTabBtns[i] != null) {
					mTabBtns[i].setOnClickListener(v -> onTabClicked(pos));
				}
			}

			// ==================== 切换到默认 Tab (BROWSE) ====================
			selectTab(TAB_BROWSE, false);

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

	// ======================================================================
	// 底部 Tab 容器 + 点击逻辑
	// ======================================================================

	/**
	 * 每个内容 Tab 容器结构：
	 *   LinearLayout vertical (MATCH_PARENT, id 动态生成)
	 *     ├─ header (HyperOS 大标题区域，可选)
	 *     │     LinearLayout horizontal MATCH_PARENT wrap_content
	 *     │       ├─ 音乐 note icon (24dp，可选，无图标时仅显示文字)
	 *     │       └─ LinearLayout vertical
	 *     │            ├─ TextView 大标题 "便携乐谱" (28sp bold)
	 *     │            └─ TextView 副标题 (14sp gray)
	 *     └─ FrameLayout (0dp weight=1) ← TabXxxList 会被 add 到这里
	 */
	private FrameLayout createTabContentWithHeader(int subtitleResId, boolean showBigTitle) {
		// 外层 FrameLayout（切换用容器）
		FrameLayout outer = new FrameLayout(this);
		outer.setLayoutParams(newFrameParams());

		// 内部 vertical linear：标题 + 内容
		LinearLayout inner = new LinearLayout(this);
		inner.setOrientation(LinearLayout.VERTICAL);
		FrameLayout.LayoutParams innerLp = newFrameParams();
		innerLp.leftMargin = dp(20);
		innerLp.rightMargin = dp(20);
		innerLp.topMargin = dp(20);
		inner.setLayoutParams(innerLp);

		// -------- Header --------
		LinearLayout header = new LinearLayout(this);
		header.setOrientation(LinearLayout.VERTICAL);
		LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		headerLp.bottomMargin = dp(16);
		header.setLayoutParams(headerLp);

		if (showBigTitle) {
			TextView bigTitle = new TextView(this);
			bigTitle.setText(R.string.home_big_title);
			bigTitle.setTextSize(32);
			bigTitle.setTypeface(null, android.graphics.Typeface.BOLD);
			bigTitle.setTextColor(0xFF212121); // hyperos_text_primary
			header.addView(bigTitle, new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

			// 副标题 + 小下划线（HyperOS 风格）
			LinearLayout subWrap = new LinearLayout(this);
			subWrap.setOrientation(LinearLayout.VERTICAL);
			LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			subLp.topMargin = dp(4);
			subWrap.setLayoutParams(subLp);

			TextView sub = new TextView(this);
			sub.setText(subtitleResId);
			sub.setTextSize(15);
			sub.setTextColor(0xFF757575); // hyperos_text_secondary
			subWrap.addView(sub, new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

			View underline = new View(this);
			LinearLayout.LayoutParams ulLp = new LinearLayout.LayoutParams(dp(40), dp(3));
			ulLp.topMargin = dp(6);
			underline.setLayoutParams(ulLp);
			underline.setBackgroundColor(0xFF5C6BC0); // hyperos_primary
			subWrap.addView(underline);

			header.addView(subWrap);
		}
		inner.addView(header);

		// -------- 内容区：FrameLayout (用 id 标识，addView 时好找) --------
		FrameLayout contentSlot = new FrameLayout(this);
		contentSlot.setId(View.generateViewId());
		contentSlot.setTag("content_slot");
		LinearLayout.LayoutParams slotLp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
		contentSlot.setLayoutParams(slotLp);
		inner.addView(contentSlot);

		outer.addView(inner);
		return outer;
	}

	/** 把 TabXxxList 或其他 View 塞进对应 FrameLayout container 的 content slot 里 */
	private void addIntoContentContainer(FrameLayout container, View child) {
		View inner = container.getChildAt(0); // LinearLayout(header + slot)
		if (inner instanceof ViewGroup) {
			ViewGroup innerVg = (ViewGroup) inner;
			for (int i = 0; i < innerVg.getChildCount(); i++) {
				View v = innerVg.getChildAt(i);
				if ("content_slot".equals(v.getTag())) {
					FrameLayout slot = (FrameLayout) v;
					slot.addView(child, new FrameLayout.LayoutParams(
							FrameLayout.LayoutParams.MATCH_PARENT,
							FrameLayout.LayoutParams.MATCH_PARENT));
					return;
				}
			}
		}
	}

	private FrameLayout.LayoutParams newFrameParams() {
		return new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.MATCH_PARENT,
				FrameLayout.LayoutParams.MATCH_PARENT);
	}

	private int dp(int value) {
		float d = getResources().getDisplayMetrics().density;
		return (int) (value * d + 0.5f);
	}

	/** 点击底部 Tab 触发 */
	private void onTabClicked(int pos) {
		if (pos == TAB_SETTINGS) {
			// 设置 Tab：不切换内容，直接启动 Settings Activity（并保持视觉高亮一下）
			Intent intent = new Intent();
			intent.setClass(this, AppPreferenceActivity.class);
			startActivity(intent);
			// Settings 虽然是新 Activity，但回到主界面时保持选中态让用户一致
			postRefreshSettingsPillHighlight();
			return;
		}
		selectTab(pos, true);
	}

	/** onResume 后刷新设置按钮的药丸高亮（因为当前内容不是设置页，所以切回 BROWSE） */
	private void postRefreshSettingsPillHighlight() {
		// 当用户从 Settings 返回时，onResume 会重新选中当前 Tab，无需额外处理
	}

	@Override
	protected void onResume() {
		super.onResume();
		// 从设置/其他页回来时，重新应用当前 Tab 的药丸高亮状态
		applyPillCheckedState(mCurrentTab);
		// 切回浏览页 / 最近 / 收藏时刷新
		if (mCurrentTab == TAB_BROWSE && tbl != null) tbl.refreshForCurrentAccessMode();
		if (mCurrentTab == TAB_RECENT && trl != null) trl.getFileDir();
		if (mCurrentTab == TAB_FAVORITE && tfl != null) tfl.getFileDir();
	}

	/**
	 * 切换 Tab（HyperOS 药丸高亮 + 内容区显隐）
	 * @param pos Tab 序号 0..3（BROWSE/RECENT/ONLINE/FAVORITE）
	 * @param animate 是否使用过渡动画（这里直接淡入淡出即可）
	 */
	private void selectTab(int pos, boolean animate) {
		if (pos < 0 || pos > 3) return; // 设置 Tab 不走这里
		mCurrentTab = pos;

		// 1. 内容区：只有当前 Tab 的容器 VISIBLE，其余 GONE
		setVisibilityOrGone(mBrowseContainer,   pos == TAB_BROWSE);
		setVisibilityOrGone(mRecentContainer,   pos == TAB_RECENT);
		setVisibilityOrGone(mOnlineContainer,   pos == TAB_ONLINE);
		setVisibilityOrGone(mFavoriteContainer, pos == TAB_FAVORITE);

		// 2. HyperOS 药丸高亮：选中 tab_pill.setChecked(true)，其余 false
		applyPillCheckedState(pos);

		// 3. 切回浏览 Tab 时尝试刷新（Android13+ 可能刚授权了目录）
		if (pos == TAB_BROWSE && tbl != null) tbl.refreshForCurrentAccessMode();
		if (pos == TAB_RECENT && trl != null) trl.getFileDir();
		if (pos == TAB_FAVORITE && tfl != null) tfl.getFileDir();
	}

	/** 根据当前 Tab 设置药丸的 checked 状态（触发 bottom_nav_item_bg selector） */
	private void applyPillCheckedState(int activePos) {
		// 5 个 Tab（含设置）：设置按钮也参与高亮
		for (int i = 0; i < 5; i++) {
			if (mTabPills[i] != null) {
				mTabPills[i].setSelected(i == activePos);
				mTabPills[i].setActivated(i == activePos);
				mTabPills[i].setPressed(false);
				// 自定义一个"选中"的状态
				mTabPills[i].setEnabled(true);
				// 由于 selector 用的是 state_checked，手动设置 isChecked 并不存在，
				// 我们直接用 setSelected(true) 配合修改过的 selector
			}
			if (mTabBtns[i] != null) {
				mTabBtns[i].setSelected(i == activePos);
			}
		}
	}

	private void setVisibilityOrGone(View v, boolean visible) {
		if (v == null) return;
		v.setVisibility(visible ? View.VISIBLE : View.GONE);
	}

	// ======================================================================
	// 系统选择器 & 权限
	// ======================================================================

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
		DebugLog.i(TAG, "onActivityResult  rc=" + requestCode + "  resultCode=" + resultCode
				+ "  data=" + (data == null ? "null" : data));
		DebugLog.d(TAG, "  extras=" + dumpBundle(data == null ? null : data.getExtras()));

		// MANAGE_EXTERNAL_STORAGE 授权回来
		if (requestCode == REQ_MANAGE_ALL_FILES) {
			if (isExternalStorageManager()) {
				Toast.makeText(this, R.string.browse_root_permission_granted, Toast.LENGTH_SHORT).show();
				if (tbl != null) tbl.refreshForCurrentAccessMode();
			} else {
				Toast.makeText(this, R.string.browse_root_permission_denied, Toast.LENGTH_LONG).show();
			}
			return;
		}

		if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
			DebugLog.w(TAG, "onActivityResult 提前 return: " +
					"resultCode!=" + Activity.RESULT_OK + " 或 data==null 或 data.getData()==null");
			return;
		}
		Uri uri = data.getData();
		try {
			if (requestCode == REQ_OPEN_MUSICXML_TREE
					&& Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				DebugLog.i(TAG, "SAF 目录授权：REQ_OPEN_MUSICXML_TREE  uri=" + uri);
				// 持久化该目录授权
				int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION
						| Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
				try {
					getContentResolver().takePersistableUriPermission(uri, takeFlags);
					DebugLog.i(TAG, "  takePersistableUriPermission 成功 flags=" + takeFlags);
				} catch (Throwable t) {
					DebugLog.w(TAG, "  takePersistableUriPermission 失败（非持久化）", t);
				}
				if (tbl != null) tbl.bindToDocumentTree(uri);
				return;
			}
			if (requestCode == REQ_OPEN_MUSICXML_FILE) {
				DebugLog.i(TAG, "SAF 单文件：REQ_OPEN_MUSICXML_FILE  uri=" + uri
						+ "  scheme=" + uri.getScheme() + "  mime=" + getContentResolver().getType(uri));
				String openedPath = SafeFileResolver.materializeToCacheFile(this, uri);
				DebugLog.i(TAG, "  materializeToCacheFile => " + openedPath);
				if (openedPath == null) {
					Toast.makeText(this, R.string.info_open_err, Toast.LENGTH_LONG).show();
					return;
				}
				if (dbhelp != null) {
					try {
						dbhelp.insertRecentItem(openedPath);
						DebugLog.d(TAG, "  recent DB 插入成功");
					} catch (Throwable t) {
						DebugLog.w(TAG, "  recent DB 插入失败", t);
					}
				}
				TabBrowseList.launchGraphicsActivity(this, new File(openedPath));
			}
		} catch (Throwable t) {
			DebugLog.e(TAG, "onActivityResult 异常", t);
			Toast.makeText(this, getString(R.string.info_open_err) + " " + t.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}

	/** 把 bundle 所有 key-val 打印出来（调试用）。null-safe。 */
	private static String dumpBundle(android.os.Bundle b) {
		if (b == null) return "(no extras)";
		try {
			StringBuilder sb = new StringBuilder(512).append('{');
			boolean first = true;
			for (String k : b.keySet()) {
				if (!first) sb.append(", ");
				first = false;
				Object v = b.get(k);
				sb.append(k).append('=').append(v == null ? "null" : v.toString());
			}
			sb.append('}');
			return sb.toString();
		} catch (Throwable t) {
			return t.toString();
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

	// -------------------------------------------------------------------
	// MANAGE_EXTERNAL_STORAGE (Android 11+ 根目录访问)
	// -------------------------------------------------------------------

	@SuppressWarnings("deprecation")
	public boolean isExternalStorageManager() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false;
		try {
			return Environment.isExternalStorageManager();
		} catch (Throwable t) {
			return false;
		}
	}

	/**
	 * 适配 HyperOS 3 / MIUI 状态栏高度。
	 */
	private void applyStatusBarInsets() {
		View rootView = findViewById(android.R.id.content);
		if (rootView == null) return;

		// 动态监听 WindowInsets
		ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
			int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
			if (statusBarHeight > 0) applyTopPadding(statusBarHeight);
			return insets;
		});

		// 同步兜底：系统资源读取
		int statusBarHeight = getStatusBarHeight();
		if (statusBarHeight > 0) applyTopPadding(statusBarHeight);
	}

	private int getStatusBarHeight() {
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			try {
				return getResources().getDimensionPixelSize(resourceId);
			} catch (Throwable ignore) {}
		}
		float density = getResources().getDisplayMetrics().density;
		return (int) (24 * density + 0.5f);
	}

	private void applyTopPadding(int top) {
		ViewGroup contentRoot = findViewById(android.R.id.content);
		if (contentRoot != null && contentRoot.getChildCount() > 0) {
			View mainRoot = contentRoot.getChildAt(0);
			mainRoot.setPadding(mainRoot.getPaddingLeft(), top,
					mainRoot.getPaddingRight(), mainRoot.getPaddingBottom());
		}
	}

	public void requestManageExternalStorage() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return;
		try {
			Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
			intent.setData(Uri.parse("package:" + getPackageName()));
			startActivityForResult(intent, REQ_MANAGE_ALL_FILES);
		} catch (Throwable t) {
			try {
				Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
				startActivityForResult(intent, REQ_MANAGE_ALL_FILES);
			} catch (Throwable t2) {
				Toast.makeText(this, R.string.browse_root_failed, Toast.LENGTH_LONG).show();
			}
		}
	}

	public class LoadThread extends Thread {
		public android.content.Context context;
		public void run() {
			SoundPoolUtiil.loadSound(context);
		}
	}

	/**
	 * UI 重构后不再显示 Options Menu（去除标题栏和三点菜单、设置做成底部 Tab）
	 * 保持方法空实现即可（避免子类意外继承引发问题）。
	 */
	@Override
	public boolean onCreateOptionsMenu(android.view.Menu menu) {
		return false; // 彻底不显示任何菜单
	}

	@Override
	public boolean onOptionsItemSelected(android.view.MenuItem item) {
		return false;
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

	// =====================================================================
	// 以下不变：MediaStore 零权限聚合、URI 归一化等工具方法
	// =====================================================================

	public static final class MediaStoreMusicXmlEntry {
		public final String displayName;
		public final String absolutePath;
		public final long   id;
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
		}
		return out;
	}

	public static File materializeUriToCacheFileSafe(@NonNull Context ctx, @NonNull Uri src) {
		try {
			String path = SafeFileResolver.materializeToCacheFile(ctx, src);
			return path == null ? null : new File(path);
		} catch (Throwable t) {
			return null;
		}
	}

	@SuppressWarnings("deprecation")
	public static File getPrimaryExternalRootCompat() {
		try {
			return Environment.getExternalStorageDirectory();
		} catch (Throwable t) {
			return new File("/");
		}
	}
}
