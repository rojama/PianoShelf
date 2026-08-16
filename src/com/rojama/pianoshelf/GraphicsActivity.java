package com.rojama.pianoshelf;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
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
 */
public class GraphicsActivity extends AppCompatActivity {
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

		String filepath = resolveFilePath(getIntent());
		pendingPath = filepath;

		if (ensureStoragePermission()) {
			initGraphicsView(filepath);
		}
	}

	/**
	 * 从 Intent 中提取乐谱文件路径，按优先级依次尝试：
	 *   1) EXTRA_FILE_PATH (内部启动直接传路径，最可靠)
	 *   2) file:// URI → getPath()
	 *   3) content:// URI → SafeFileResolver 复制到 cache 后返回路径
	 */
	@Nullable
	private String resolveFilePath(@Nullable Intent intent) {
		if (intent == null) return null;

		// 1) 内部 extra
		String extraPath = intent.getStringExtra(EXTRA_FILE_PATH);
		if (extraPath != null && new File(extraPath).isFile()) {
			return extraPath;
		}

		// 2) / 3) URI
		Uri uri = intent.getData();
		if (uri == null) return null;

		String scheme = uri.getScheme();
		if ("file".equals(scheme)) {
			try {
				return new File(new URI(uri.toString())).getPath();
			} catch (URISyntaxException | IllegalArgumentException e) {
				e.printStackTrace();
			}
			return null;
		}

		if ("content".equals(scheme)) {
			// FileProvider / SAF content:// URI → 复制到 cache 再返回路径
			String cached = SafeFileResolver.materializeToCacheFile(this, uri);
			if (cached == null) {
				Toast.makeText(this, R.string.info_open_err, Toast.LENGTH_LONG).show();
			}
			return cached;
		}

		return null;
	}

	private boolean ensureStoragePermission() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
		int r = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
		if (r == PackageManager.PERMISSION_GRANTED) return true;
		ActivityCompat.requestPermissions(this,
				new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE },
				REQ_STORAGE);
		return false;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == REQ_STORAGE && grantResults.length > 0
				&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			initGraphicsView(pendingPath);
		}
	}

	private void initGraphicsView(@Nullable String filepath) {
		if (filepath == null) return;
		graphicsView = new GraphicsView(GraphicsActivity.this);
		graphicsView.filepath = filepath;
		DisplayMetrics dm = getResources().getDisplayMetrics();
		graphicsView.screenWidth = dm.widthPixels;
		graphicsView.screenHeight = dm.heightPixels;
		graphicsView.showView();
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
				graphicsView.reShowView();
			}
			break;
		case 2: // page down
			if (graphicsView.ct != null && graphicsView.dispalyPageNo < graphicsView.ct.maxPage) {
				graphicsView.olddispalyPageNo = graphicsView.dispalyPageNo;
				graphicsView.dispalyPageNo++;
				graphicsView.reShowView();
			}
			break;
		case 3: // play / toggle
			if (graphicsView.isPlaying()) {
				graphicsView.stopPlayback();
			} else {
				graphicsView.play();
			}
			invalidateOptionsMenu();
			break;
		case 4: // stop
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
			graphicsView.changeOrientation();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (graphicsView != null) graphicsView.stopPlayback();
	}

	@Override
	protected void onDestroy() {
		if (graphicsView != null) {
			graphicsView.shutdown();
			graphicsView = null;
		}
		SoundPoolUtiil.release();
		super.onDestroy();
	}
}
