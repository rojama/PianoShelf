package com.rojama.pianoshelf;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * 乐谱查看 Activity (Modernized)
 *
 * 修复:
 *  - 继承 AppCompatActivity (替代纯 Activity, 获得 ActionBar/Toolbar 支持)
 *  - 屏幕尺寸: Display.getWidth/Height 废弃 → DisplayMetrics
 *  - 生命周期: onDestroy 释放 GraphicsView (播放线程池 + SoundPool)
 *  - 权限: Android 6.0+ 动态检查 READ/WRITE 存储权限
 *  - 菜单启用判断: 增加 null-safe
 *  - URI 处理: 支持 content:// (FileProvider/SAF) 和 file:// 两种 scheme
 *    旧代码 new File(new URI("content://...")) 会抛 IllegalArgumentException
 *    导致 filepath=null → .xml/.mxl 全部打不开
 *  - 调试: 进入 Activity 立刻绑定 DebugLog 面板 + 写入 HEADER，不管 filepath/权限是否失败
 */
public class GraphicsActivity extends AppCompatActivity {
	private static final String TAG = "Graphics";
	private static final int REQ_STORAGE = 1001;
	/** 内部启动时直接传文件路径，避免 FileProvider URI 往返转换 */
	public static final String EXTRA_FILE_PATH = "com.rojama.pianoshelf.FILE_PATH";
	private GraphicsView graphicsView = null;
	private String pendingPath = null;

	// 底部播放控制条
	private android.widget.TextView btnPrevPage;
	private android.widget.TextView btnPlayPause;
	private android.widget.TextView btnNextPage;
	private android.widget.SeekBar seekPlayback;
	private android.widget.TextView tvPageInfo;

	/** 防止重复给 TouchView 绑定 PageFlipListener。 */
	private boolean pageFlipHooked = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.shelf);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		DebugLog.ensureInitialized(this);
		DebugLog.i(TAG, "========== GraphicsActivity.onCreate 启动 ==========");
		writeSessionHeader(getIntent());

		// 无论 filepath 是不是 null，setContentView 之后都先把 UI 组件初始化一次并绑定日志面板，
		// 这样即使后面早退出 (filepath null / 权限拒绝)，用户也能看到完整 HEADER + 失败原因日志。
		ensureUIBound();

		DisplayMetrics dm = getResources().getDisplayMetrics();
		DebugLog.i(TAG, "屏幕宽高=" + dm.widthPixels + "x" + dm.heightPixels
				+ " density=" + dm.density + " densityDpi=" + dm.densityDpi
				+ " orientation=" + getResources().getConfiguration().orientation);

		String filepath = resolveFilePath(getIntent());
		pendingPath = filepath;
		DebugLog.i(TAG, "resolveFilePath 返回 => " + filepath);

		if (filepath == null) {
			DebugLog.e(TAG, "filepath == null，将不启动 initGraphicsView，直接显示失败提示");
			TextView pt = findViewById(R.id.progressText);
			if (pt != null) {
				pt.setText("❌ 未拿到文件路径（点击查看下方日志面板）");
				pt.setTextColor(0xFFD50000);
			}
			showLogPanel();
			Toast.makeText(this, R.string.info_open_err + " (resolveFilePath 返回 null)", Toast.LENGTH_LONG).show();
			return;
		}

		// 因为我们已经将所有文件通过 ContentResolver 复制到了应用的 cache 目录，
		// 读取 cache 目录下的文件不需要任何存储权限。因此这里跳过 ensureStoragePermission 检查。
		File checkFile = new File(filepath);
		if (checkFile.exists() && checkFile.canRead()) {
			DebugLog.i(TAG, "文件存在且可读，直接 initGraphicsView（跳过权限检查）");
			initGraphicsView(filepath, dm.widthPixels, dm.heightPixels);
		} else {
			DebugLog.e(TAG, "文件不存在或不可读，中止加载");
			TextView pt = findViewById(R.id.progressText);
			if (pt != null) {
				pt.setText("❌ 文件不存在或不可读：" + filepath);
				pt.setTextColor(0xFFD50000);
			}
			showLogPanel();
		}
	}

	/** 面板高度/折叠模式 */
	private enum LogPanelMode {
		/** 完全隐藏 */
		HIDDEN,
		/** 底部半高（默认 280dp max） */
		HALF,
		/** 全屏覆盖 */
		FULL
	}
	private LogPanelMode logMode = LogPanelMode.HALF;
	/** 切换全屏时记住上次半高的 maxHeight，便于还原 */
	private int logHalfMaxPx = 0;
	/** ScrollView 的 maxHeight 属性需要通过 LayoutParams 动态控制，所以这里存个引用 */
	private ScrollView logScrollRef = null;
	private View logPanelRef = null;

	/** 把 GraphicsView 绑定到 UI 控件这件事独立出来：即使还没 filepath 也要先让日志面板跑起来。 */
	private void ensureUIBound() {
		try {
			// 注意：GraphicsView 此时可能是 null → 我们直接构造一个临时 GraphicsView 去完成 UI 绑定（wireDebugLogPanel + 进度文字显隐切换）。
			// 但是 GraphicsView(Context) 还需要 Activity，所以给它一个假的 filepath=null，只是用来做 UI 绑定。
			if (graphicsView == null) {
				graphicsView = new GraphicsView(GraphicsActivity.this);
				DisplayMetrics dm = getResources().getDisplayMetrics();
				graphicsView.screenWidth = dm.widthPixels;
				graphicsView.screenHeight = dm.heightPixels;
			}

			// ===== 底部播放控制条绑定 =====
			btnPrevPage = findViewById(R.id.btn_prev_page);
			btnPlayPause = findViewById(R.id.btn_play_pause);
			btnNextPage = findViewById(R.id.btn_next_page);
			seekPlayback = findViewById(R.id.seek_playback);
			tvPageInfo = findViewById(R.id.tv_page_info);

			if (btnPrevPage != null) {
				btnPrevPage.setOnClickListener(v -> doPrevPage());
			}
			if (btnNextPage != null) {
				btnNextPage.setOnClickListener(v -> doNextPage());
			}
			if (btnPlayPause != null) {
				btnPlayPause.setOnClickListener(v -> togglePlayback());
			}
			if (seekPlayback != null) {
				// 初始禁用；播放开始后启用
				seekPlayback.setEnabled(false);
				seekPlayback.setProgress(0);
			}
			updatePageInfo(1, 1);
			updatePlayPauseIcon(false);

			// GraphicsView → Activity 的回调：页码 / 播放状态 / 进度
			graphicsView.setPlaybackStateListener(new GraphicsView.PlaybackStateListener() {
				@Override public void onPageChanged(int currentPage, int maxPage) {
					updatePageInfo(currentPage, maxPage);
					hookTouchViewPageFlipIfNeeded();
				}
				@Override public void onPlayStateChanged(boolean isPlaying) {
					updatePlayPauseIcon(isPlaying);
					if (seekPlayback != null) seekPlayback.setEnabled(isPlaying);
				}
				@Override public void onPlaybackProgress(int currentTick, int maxTick) {
					if (seekPlayback == null) return;
					int ratio = (int) ((long) currentTick * seekPlayback.getMax() / Math.max(1, maxTick));
					seekPlayback.setProgress(ratio);
				}
			});

			logPanelRef = findViewById(R.id.debug_log_panel);
			logScrollRef = findViewById(R.id.logScroll);
			logHalfMaxPx = Math.round(280 * getResources().getDisplayMetrics().density);

			// 点击进度文字：切换日志面板可见 / 隐藏
			View pt = findViewById(R.id.progressText);
			if (pt != null) {
				pt.setOnClickListener(v -> {
					View panel = findViewById(R.id.debug_log_panel);
					if (panel == null) return;
					if (panel.getVisibility() == View.VISIBLE) {
						panel.setVisibility(View.GONE);
						logMode = LogPanelMode.HIDDEN;
						DebugLog.i(TAG, "用户点击进度文字：隐藏日志面板");
					} else {
						panel.setVisibility(View.VISIBLE);
						applyLogMode(LogPanelMode.HALF);
						DebugLog.i(TAG, "用户点击进度文字：显示日志面板");
						// 切到 VISIBLE 后让日志面板滚到底
						final android.widget.ScrollView sv = findViewById(R.id.logScroll);
						if (sv != null) sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
					}
				});
			}

			// 关闭按钮
			View closeBtn = findViewById(R.id.btn_log_close);
			if (closeBtn != null) {
				closeBtn.setOnClickListener(v -> {
					View panel = findViewById(R.id.debug_log_panel);
					if (panel == null) return;
					panel.setVisibility(View.GONE);
					logMode = LogPanelMode.HIDDEN;
					DebugLog.i(TAG, "用户点击 × ：关闭日志面板");
				});
			}

			// 展开/全屏切换按钮（⤢）：循环 HALF → FULL → HALF
			View toggleBtn = findViewById(R.id.btn_log_toggle);
			if (toggleBtn != null) {
				toggleBtn.setOnClickListener(v -> {
					View panel = findViewById(R.id.debug_log_panel);
					if (panel == null) return;
					if (panel.getVisibility() != View.VISIBLE) {
						panel.setVisibility(View.VISIBLE);
						applyLogMode(LogPanelMode.HALF);
						return;
					}
					if (logMode == LogPanelMode.FULL) {
						applyLogMode(LogPanelMode.HALF);
						DebugLog.i(TAG, "用户点击 ⤢ ：面板切回半高");
					} else {
						applyLogMode(LogPanelMode.FULL);
						DebugLog.i(TAG, "用户点击 ⤢ ：面板切到全屏");
					}
				});
			}

			// 点击标题栏（非按钮区域）：折叠仅显示标题
			View titleBar = findViewById(R.id.log_title_bar);
			View titleTv = findViewById(R.id.log_title);
			if (titleBar != null && logScrollRef != null) {
				titleBar.setOnClickListener(v -> {
					if (logScrollRef.getVisibility() == View.GONE) {
						logScrollRef.setVisibility(View.VISIBLE);
						applyLogMode(logMode == LogPanelMode.FULL ? LogPanelMode.FULL : LogPanelMode.HALF);
						DebugLog.i(TAG, "用户点击标题栏：展开日志内容区");
					} else {
						logScrollRef.setVisibility(View.GONE);
						DebugLog.i(TAG, "用户点击标题栏：折叠日志内容区（仅留标题）");
					}
				});
				// 标题 TextView 也派发点击（防止权重=1的区域不响应）
				if (titleTv != null) titleTv.setOnClickListener(v -> titleBar.callOnClick());
			}
		} catch (Throwable t) {
			DebugLog.e(TAG, "ensureUIBound 异常（面板可能未生效）", t);
		}
	}

	/**
	 * 把调试日志面板调整到指定模式：
	 *  - HIDDEN：隐藏（visibility=GONE）
	 *  - HALF：ScrollView maxHeight=280dp，面板高度=wrap_content，底部对齐
	 *  - FULL：ScrollView 无 maxHeight，面板 MATCH_PARENT，覆盖整屏并防止穿透到下面的乐谱
	 */
	private void applyLogMode(LogPanelMode target) {
		logMode = target;
		if (logPanelRef == null) logPanelRef = findViewById(R.id.debug_log_panel);
		if (logScrollRef == null) logScrollRef = findViewById(R.id.logScroll);
		if (logPanelRef == null) return;
		ViewGroup.LayoutParams lp = logPanelRef.getLayoutParams();
		switch (target) {
			case HIDDEN:
				logPanelRef.setVisibility(View.GONE);
				break;
			case HALF:
				logPanelRef.setVisibility(View.VISIBLE);
				if (lp != null) {
					lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
					lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
					logPanelRef.setLayoutParams(lp);
				}
				logPanelRef.setClickable(false); // 半高时不拦截点击（底部以上仍可操作乐谱）
				if (logScrollRef != null) {
					ViewGroup.LayoutParams slp = logScrollRef.getLayoutParams();
					if (slp != null) {
						// ScrollView 原生没有 setMaxHeight；直接固定一个 px 高度当"半高"
						int halfPx = logHalfMaxPx > 0 ? logHalfMaxPx : Math.round(280 * getResources().getDisplayMetrics().density);
						slp.height = halfPx;
						if (slp instanceof LinearLayout.LayoutParams) {
							((LinearLayout.LayoutParams) slp).weight = 0f;
						}
						logScrollRef.setLayoutParams(slp);
					}
					logScrollRef.setVisibility(View.VISIBLE);
				}
				break;
			case FULL:
				logPanelRef.setVisibility(View.VISIBLE);
				DisplayMetrics dm2 = getResources().getDisplayMetrics();
				if (lp != null) {
					lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
					lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
					logPanelRef.setLayoutParams(lp);
				}
				logPanelRef.setClickable(true); // 全屏时吃掉所有点击，避免误触下方乐谱
				if (logScrollRef != null) {
					ViewGroup.LayoutParams slp = logScrollRef.getLayoutParams();
					if (slp != null) {
						slp.height = 0;
						// layout_weight=1 效果用 LinearLayout.LayoutParams 实现
						if (slp instanceof LinearLayout.LayoutParams) {
							((LinearLayout.LayoutParams) slp).weight = 1f;
						}
						logScrollRef.setLayoutParams(slp);
					}
					logScrollRef.setVisibility(View.VISIBLE);
				}
				break;
		}
		final ScrollView sv = findViewById(R.id.logScroll);
		if (sv != null) sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
	}

	/**
	 * 物理返回键：按用户的真实使用顺序：
	 *   - 若面板是 FULL → 先退回 HALF（用户最常用）
	 *   - 若面板是 HALF/可见 → 先隐藏面板
	 *   - 面板已隐藏 → 交给系统正常 finish Activity
	 */
	@Override
	public void onBackPressed() {
		View panel = findViewById(R.id.debug_log_panel);
		if (panel != null && panel.getVisibility() == View.VISIBLE) {
			if (logMode == LogPanelMode.FULL) {
				applyLogMode(LogPanelMode.HALF);
				DebugLog.i(TAG, "用户按返回键：面板从全屏切回半高");
				return;
			}
			panel.setVisibility(View.GONE);
			logMode = LogPanelMode.HIDDEN;
			DebugLog.i(TAG, "用户按返回键：隐藏日志面板");
			return;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			// API 34+ 系统推荐回调方式，这里兜底：直接 finish 以免卡住
			super.onBackPressed();
		} else {
			super.onBackPressed();
		}
	}

	/** 兼容：老设备 KeyEvent.KEYCODE_BACK 也走同一套逻辑 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			View panel = findViewById(R.id.debug_log_panel);
			if (panel != null && panel.getVisibility() == View.VISIBLE) {
				// 交给 onBackPressed（生命周期更完整）：手动触发一次
				try {
					onBackPressed();
					return true;
				} catch (Throwable ignore) { /* 极少数系统在 onKeyDown 禁止递归调用 onBackPressed */ }
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private void writeSessionHeader(Intent intent) {
		try {
			PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
			DebugLog.i(TAG, String.format(
					"App 版本: pkg=%s version=%s verCode=%d",
					getPackageName(), pi.versionName,
					Build.VERSION.SDK_INT >= 28 ? pi.getLongVersionCode() : pi.versionCode));
		} catch (Throwable t) {
			DebugLog.e(TAG, "读取 PackageInfo 失败", t);
		}
		DebugLog.i(TAG, "Android 版本: SDK=" + Build.VERSION.SDK_INT
				+ "  RELEASE=" + Build.VERSION.RELEASE + "  CODENAME=" + Build.VERSION.CODENAME
				+ "  MANUFACTURER=" + Build.MANUFACTURER + "  BRAND=" + Build.BRAND
				+ "  MODEL=" + Build.MODEL + "  DEVICE=" + Build.DEVICE + "  PRODUCT=" + Build.PRODUCT);
		String uriStr = (intent == null || intent.getData() == null) ? "null" : intent.getData().toString();
		String mime = (intent == null) ? "null" : intent.getType();
		DebugLog.i(TAG, "Intent.getData=" + uriStr + "  type=" + mime);
		DebugLog.i(TAG, "Intent.action=" + (intent == null ? "null" : intent.getAction()));
		DebugLog.d(TAG, "Intent.extras=" + dumpBundle(intent == null ? null : intent.getExtras()));
		String logFile = DebugLog.getLogFilePath();
		DebugLog.i(TAG, "持久化 debug.log：" + (logFile == null ? "(未写入，应用级外部存储不可用)" : logFile));
	}

	/**
	 * 从 Intent 中提取乐谱文件路径，按优先级依次尝试：
	 *   1) EXTRA_FILE_PATH (内部启动直接传路径，最可靠)
	 *   2) file:// URI → getPath()
	 *   3) content:// URI → SafeFileResolver 复制到 cache 后返回路径
	 */
	@Nullable
	private String resolveFilePath(@Nullable Intent intent) {
		DebugLog.d(TAG, "[resolveFilePath] 开始  intent=" + intent);
		if (intent == null) {
			DebugLog.w(TAG, "[resolveFilePath] intent==null => return null");
			return null;
		}

		// 1) 内部 extra
		String extraPath = intent.getStringExtra(EXTRA_FILE_PATH);
		DebugLog.d(TAG, "[resolveFilePath] (1) EXTRA_FILE_PATH=" + extraPath);
		if (extraPath != null) {
			File f = new File(extraPath);
			DebugLog.d(TAG, "[resolveFilePath]   exists=" + f.exists() + " canRead=" + f.canRead()
					+ " isFile=" + f.isFile() + " size=" + (f.isFile() ? f.length() : -1));
			if (f.isFile()) {
				DebugLog.i(TAG, "[resolveFilePath] 命中 EXTRA_FILE_PATH => " + extraPath);
				return extraPath;
			}
			DebugLog.w(TAG, "[resolveFilePath] EXTRA_FILE_PATH 不是个文件，继续尝试 URI");
		}

		// 2) / 3) URI
		Uri uri = intent.getData();
		DebugLog.d(TAG, "[resolveFilePath] (2/3) getData=" + uri);
		if (uri == null) {
			DebugLog.w(TAG, "[resolveFilePath] 没有 URI，返回 null");
			return null;
		}

		String scheme = uri.getScheme();
		DebugLog.d(TAG, "[resolveFilePath]   scheme=" + scheme);
		if ("file".equals(scheme)) {
			try {
				File f = new File(new URI(uri.toString()));
				DebugLog.i(TAG, "[resolveFilePath] file:// URI → path=" + f
						+ " exists=" + f.exists() + " isFile=" + f.isFile());
				return f.getPath();
			} catch (URISyntaxException | IllegalArgumentException e) {
				DebugLog.e(TAG, "[resolveFilePath] file:// URI 解析失败", e);
			}
			return null;
		}

		if ("content".equals(scheme)) {
			DebugLog.i(TAG, "[resolveFilePath] content:// URI → 走 SafeFileResolver");
			String cached = SafeFileResolver.materializeToCacheFile(this, uri);
			DebugLog.i(TAG, "[resolveFilePath] SafeFileResolver => " + cached);
			if (cached == null) {
				Toast.makeText(this, R.string.info_open_err, Toast.LENGTH_LONG).show();
			}
			return cached;
		}

		DebugLog.w(TAG, "[resolveFilePath] 不支持的 scheme: " + scheme + "，返回 null");
		return null;
	}

	private boolean ensureStoragePermission() {
		DebugLog.d(TAG, "[ensureStoragePermission] SDK=" + Build.VERSION.SDK_INT);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			DebugLog.i(TAG, "[ensureStoragePermission] SDK<M, 无需运行时权限, return true");
			return true;
		}
		int r = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
		DebugLog.d(TAG, "[ensureStoragePermission] checkSelfPermission(READ_EXT)=" + r
				+ "  (GRANTED=" + PackageManager.PERMISSION_GRANTED + ")");
		if (r == PackageManager.PERMISSION_GRANTED) return true;
		DebugLog.i(TAG, "[ensureStoragePermission] 发起 requestPermissions requestCode=" + REQ_STORAGE);
		ActivityCompat.requestPermissions(this,
				new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE },
				REQ_STORAGE);
		return false;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		DebugLog.i(TAG, "onRequestPermissionsResult  rc=" + requestCode
				+ "  perms=" + java.util.Arrays.toString(permissions)
				+ "  results=" + java.util.Arrays.toString(grantResults));
		if (requestCode == REQ_STORAGE && grantResults.length > 0
				&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			DisplayMetrics dm = getResources().getDisplayMetrics();
			initGraphicsView(pendingPath, dm.widthPixels, dm.heightPixels);
		} else if (requestCode == REQ_STORAGE) {
			DebugLog.e(TAG, "存储权限被用户拒绝，加载中止");
			TextView pt = findViewById(R.id.progressText);
			if (pt != null) {
				pt.setText("❌ 存储权限被拒绝（点击查看日志）");
				pt.setTextColor(0xFFD50000);
			}
			showLogPanel();
		}
	}

	private void initGraphicsView(@Nullable String filepath, int widthPx, int heightPx) {
		DebugLog.i(TAG, "initGraphicsView  filepath=" + filepath + "  w=" + widthPx + " h=" + heightPx);
		if (filepath == null) {
			DebugLog.e(TAG, "initGraphicsView  filepath==null，直接 return");
			showLogPanel();
			return;
		}
		// ensureUIBound 在 onCreate 已经构造过一个 graphicsView（用来绑定日志面板），这里复用
		if (graphicsView == null) {
			graphicsView = new GraphicsView(GraphicsActivity.this);
		}
		graphicsView.filepath = filepath;
		graphicsView.screenWidth = widthPx;
		graphicsView.screenHeight = heightPx;
		DebugLog.i(TAG, "调用 graphicsView.showView() →");
		graphicsView.showView();
	}

	private void showLogPanel() {
		View panel = findViewById(R.id.debug_log_panel);
		if (panel != null && panel.getVisibility() != View.VISIBLE) {
			applyLogMode(LogPanelMode.HALF);
			return;
		}
		if (logMode != LogPanelMode.FULL) applyLogMode(LogPanelMode.HALF);
		final ScrollView sv = findViewById(R.id.logScroll);
		if (sv != null) sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
	}

	private static String dumpBundle(Bundle b) {
		if (b == null) return "(no extras)";
		try {
			StringBuilder sb = new StringBuilder(1024).append('{');
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		int group1 = 1;
		menu.add(group1, 1, 1, getString(R.string.menu_pageup));
		menu.add(group1, 2, 2, getString(R.string.menu_pagedown));
		menu.add(group1, 3, 3, getString(R.string.menu_play));
		menu.add(group1, 4, 4, getString(R.string.menu_stop));
		return true;
	}

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		if (graphicsView == null) return super.onMenuOpened(featureId, menu);
		boolean firstPage = graphicsView.dispalyPageNo <= 1;
		boolean lastPage = (graphicsView.ct == null) || (graphicsView.dispalyPageNo >= graphicsView.ct.maxPage);
		if (menu.size() >= 2) {
			menu.getItem(0).setEnabled(!firstPage);
			menu.getItem(1).setEnabled(!lastPage);
			MenuItem playItem = menu.getItem(2);
			if (graphicsView.isPlaying()) {
				playItem.setTitle(R.string.menu_pause);
			} else {
				playItem.setTitle(R.string.menu_play);
			}
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (graphicsView == null) return super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case 1: // page up
			if (graphicsView.dispalyPageNo > 1) {
				graphicsView.olddispalyPageNo = graphicsView.dispalyPageNo;
				graphicsView.dispalyPageNo--;
				DebugLog.i(TAG, "菜单: 上一页 " + graphicsView.dispalyPageNo);
				graphicsView.reShowView();
			}
			break;
		case 2: // page down
			if (graphicsView.ct != null && graphicsView.dispalyPageNo < graphicsView.ct.maxPage) {
				graphicsView.olddispalyPageNo = graphicsView.dispalyPageNo;
				graphicsView.dispalyPageNo++;
				DebugLog.i(TAG, "菜单: 下一页 " + graphicsView.dispalyPageNo);
				graphicsView.reShowView();
			}
			break;
		case 3: // play / toggle
			DebugLog.i(TAG, "菜单: 播放切换  isPlaying=" + graphicsView.isPlaying());
			if (graphicsView.isPlaying()) {
				graphicsView.stopPlayback();
			} else {
				graphicsView.play();
			}
			invalidateOptionsMenu();
			break;
		case 4: // stop
			DebugLog.i(TAG, "菜单: 停止");
			graphicsView.stopPlayback();
			break;
		}
		return true;
	}

	// ============================================================
	// 底部播放控制条 + 滑动翻页 辅助方法
	// ============================================================

	/** 上一页：按钮 / 右滑手势都会走到这里。 */
	private void doPrevPage() {
		if (graphicsView == null) return;
		if (graphicsView.getCurrentPage() <= 1) {
			Toast.makeText(this, "已经是第一页", Toast.LENGTH_SHORT).show();
			return;
		}
		graphicsView.olddispalyPageNo = graphicsView.dispalyPageNo;
		graphicsView.dispalyPageNo--;
		DebugLog.i(TAG, "翻页: 上一页 → " + graphicsView.dispalyPageNo);
		graphicsView.reShowView();
	}

	/** 下一页：按钮 / 左滑手势都会走到这里。 */
	private void doNextPage() {
		if (graphicsView == null) return;
		if (graphicsView.getCurrentPage() >= graphicsView.getMaxPage()) {
			Toast.makeText(this, "已经是最后一页", Toast.LENGTH_SHORT).show();
			return;
		}
		graphicsView.olddispalyPageNo = graphicsView.dispalyPageNo;
		graphicsView.dispalyPageNo++;
		DebugLog.i(TAG, "翻页: 下一页 → " + graphicsView.dispalyPageNo);
		graphicsView.reShowView();
	}

	/** 播放 / 暂停 切换（控制条 ⏵ / ⏸ 按钮）。 */
	private void togglePlayback() {
		if (graphicsView == null) return;
		if (graphicsView.isPlaying()) {
			DebugLog.i(TAG, "控制条: 暂停播放");
			graphicsView.stopPlayback();
		} else {
			DebugLog.i(TAG, "控制条: 开始播放");
			graphicsView.play();
		}
		invalidateOptionsMenu();
	}

	/** 刷新控制条中央的"播放/暂停"图标（⏵=待播放，⏸=播放中）。 */
	private void updatePlayPauseIcon(boolean isPlaying) {
		if (btnPlayPause == null) return;
		btnPlayPause.setText(isPlaying ? "⏸" : "⏵");
		btnPlayPause.setTextColor(isPlaying ? 0xFFC62828 : 0xFF2E7D32); // 红暂停 / 绿播放
	}

	/** 刷新"第 X / Y 页"文字，并按边界禁用按钮。 */
	private void updatePageInfo(int currentPage, int maxPage) {
		int cur = Math.max(1, currentPage);
		int max = Math.max(1, maxPage);
		if (tvPageInfo != null) {
			tvPageInfo.setText(String.format(java.util.Locale.US, "第 %d / %d 页", cur, max));
		}
		if (btnPrevPage != null) {
			btnPrevPage.setEnabled(cur > 1);
			btnPrevPage.setAlpha(cur > 1 ? 1.0f : 0.35f);
		}
		if (btnNextPage != null) {
			btnNextPage.setEnabled(cur < max);
			btnNextPage.setAlpha(cur < max ? 1.0f : 0.35f);
		}
	}

	/**
	 * TouchView 只有在 onLoadFinished 把 detail 创建好之后才存在，
	 * 所以我们在每次 onPageChanged 回调时尝试 hook 一次（成功后跳过）。
	 */
	private void hookTouchViewPageFlipIfNeeded() {
		if (pageFlipHooked || graphicsView == null) return;
		TouchView tv = graphicsView.getTouchView();
		if (tv == null) return;
		tv.setPageFlipListener(new TouchView.PageFlipListener() {
			@Override public void onPrevPage() { doPrevPage(); }
			@Override public void onNextPage() { doNextPage(); }
		});
		pageFlipHooked = true;
		DebugLog.i(TAG, "已把滑动翻页回调挂到 TouchView");
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (graphicsView != null) {
			DisplayMetrics dm = getResources().getDisplayMetrics();
			graphicsView.screenWidth = dm.widthPixels;
			graphicsView.screenHeight = dm.heightPixels;
			DebugLog.i(TAG, "屏幕旋转/配置变更 w=" + dm.widthPixels + " h=" + dm.heightPixels);
			graphicsView.changeOrientation();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		DebugLog.d(TAG, "onPause");
		if (graphicsView != null) graphicsView.stopPlayback();
	}

	@Override
	protected void onDestroy() {
		DebugLog.i(TAG, "onDestroy: 释放 GraphicsView + SoundPool");
		if (graphicsView != null) {
			graphicsView.shutdown();
			graphicsView = null;
		}
		SoundPoolUtiil.release();
		super.onDestroy();
	}
}
