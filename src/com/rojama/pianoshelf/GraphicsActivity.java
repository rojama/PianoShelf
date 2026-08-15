package com.rojama.pianoshelf;

import java.io.File;

import android.Manifest;
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
 * 乐谱查看 Activity — Scoped Storage 兼容版。
 *
 * 为什么原代码在 Android 7+ / content:// URI 下打不开文件：
 *   原实现写死为 "new File(new URI(intent.getDataString()))"。
 *     a) 对于 FileProvider 给的 content://com.rojama.pianoshelf.fileprovider/... 这种 URI，
 *        new URI(...) 再 new File(...) 会直接抛 IllegalArgumentException（不是绝对路径）
 *     b) 对于带 URL-encode 的 SAF 路径（空格变 %20 等），URISyntaxException / 找不到文件
 *     c) 就算是 file://，从 Android 13 起 app 外部目录本来就不可读
 *
 * 修复：
 *   入口统一走 SafeFileResolver.materializeToCacheFile()：
 *     - 任何 URI (content:// / file:// / SAF document / MediaStore content)
 *       都被复制到 app cache 目录一份稳定本地副本，GraphicsView 用标准 FileReader 读
 *     - 因此不再需要 READ_EXTERNAL_STORAGE：仅在用户手动导入外部 legacy 目录时才申请，
 *       而且 Android 13+ 即使申请也会被零授予，直接跳过以避免骚扰用户
 *   生命周期: onDestroy 释放 GraphicsView (播放线程池 + SoundPool)
 */
public class GraphicsActivity extends AppCompatActivity {
	private static final int REQ_STORAGE = 1001;
	private GraphicsView graphicsView = null;
	private String pendingPath = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.shelf);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		String filepath = null;
		try {
			if (getIntent() != null && getIntent().getData() != null) {
				Uri uri = getIntent().getData();
				// ↓ 关键修复：任何 URI 归一化到 app cache 里的本地可读 File
				String cached = SafeFileResolver.materializeToCacheFile(this, uri);
				if (cached != null) {
					filepath = cached;
				} else {
					// fallback: legacy path（万一缓存目录不可写，尝试读纯 file:// 形式）
					if ("file".equalsIgnoreCase(uri.getScheme()) && uri.getPath() != null) {
						File f = new File(uri.getPath());
						if (f.isFile() && f.canRead()) filepath = f.getAbsolutePath();
					}
				}
			}
		} catch (Throwable t) {
			// 防御式：URI 解析等任何异常先记录，不崩
			t.printStackTrace();
		}

		if (filepath == null) {
			// 没有任何可用路径：提示用户并退出
			Toast.makeText(this, R.string.info_open_err, Toast.LENGTH_LONG).show();
			// 不 finish，让用户至少看到界面（保持旧行为）
			return;
		}
		pendingPath = filepath;

		if (ensureStoragePermissionIfNeeded()) {
			initGraphicsView(filepath);
		}
	}

	/**
	 * 权限检查：
	 *  - Android 6-12：若用户把文件放在 /sdcard 的 legacy tree 下（非 cache，非 FileProvider），
	 *    仍需 READ_EXTERNAL_STORAGE → 动态申请
	 *  - Android 13+：该权限零授予，且我们已经 materialize 到 cache dir，不需要 → 直接放行
	 *  - < Android 6：安装时即授予 → 直接放行
	 */
	private boolean ensureStoragePermissionIfNeeded() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return true; // API 33+
		// 只有当 pendingPath 真的位于"外部存储公共目录"（非 app-private）时才需要权限
		// 简单处理：先 checkSelfPermission，授予就直接开；否则尝试申请，失败也仍尝试 init
		// （因为 cache dir 不需要权限，大多数情况都能打开）
		int r = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
		if (r == PackageManager.PERMISSION_GRANTED) return true;
		// 申请一次（Android 10-12 实际有用）
		ActivityCompat.requestPermissions(this,
				new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE },
				REQ_STORAGE);
		// 无论是否 grant，都先尝试 init（cache dir 无权限也能读）
		// 对 cache 里的 materialized file 而言一定能读，所以这里直接放行；
		// 如果是老的 legacy 绝对路径且没权限，GraphicsView 会在解析时报错 Toast。
		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		// 结果到了但我们在 ensureStoragePermissionIfNeeded 里已经尝试 init 过一次，
		// 所以这里只处理「用户取消了但 pendingPath 仍是 legacy 绝对路径」的补救场景：
		if (requestCode == REQ_STORAGE && grantResults.length > 0
				&& grantResults[0] == PackageManager.PERMISSION_GRANTED
				&& graphicsView == null) {
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
