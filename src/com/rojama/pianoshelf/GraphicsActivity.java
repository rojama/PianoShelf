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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
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

		if (ensureStoragePermission()) {
			DebugLog.i(TAG, "权限已有，直接 initGraphicsView");
			initGraphicsView(filepath, dm.widthPixels, dm.heightPixels);
		} else {
			DebugLog.w(TAG, "权限未授予，已触发 requestPermissions 弹框，等待回调中");
		}
	}

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
			// 点击进度文字：切换日志面板可见 / 隐藏
			View pt = findViewById(R.id.progressText);
			if (pt != null) {
				pt.setOnClickListener(v -> {
					View panel = findViewById(R.id.debug_log_panel);
					if (panel == null) return;
					if (panel.getVisibility() == View.VISIBLE) {
						panel.setVisibility(View.GONE);
						DebugLog.i(TAG, "用户点击进度文字：隐藏日志面板");
					} else {
						panel.setVisibility(View.VISIBLE);
						DebugLog.i(TAG, "用户点击进度文字：显示日志面板");
						// 切到 VISIBLE 后让日志面板滚到底
						final android.widget.ScrollView sv = findViewById(R.id.logScroll);
						if (sv != null) sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
					}
				});
			}
		} catch (Throwable t) {
			DebugLog.e(TAG, "ensureUIBound 异常（面板可能未生效）", t);
		}
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
			panel.setVisibility(View.VISIBLE);
		}
		final android.widget.ScrollView sv = findViewById(R.id.logScroll);
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
