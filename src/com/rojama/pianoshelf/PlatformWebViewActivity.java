package com.rojama.pianoshelf;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;

/**
 * 平台内置 WebView 浏览器（MuseScore / IMSLP / OpenScore）。
 *
 * 解决的问题：
 *   1) 之前直接跳外部浏览器，用户需要手动复制 URL 再回 App，链路长。
 *   2) 现在 App 内浏览 → 用户点击 MusicXML 下载链接 → 自动拦截 → 走 OnlineScoreDownloader。
 *   3) 非 MusicXML 的正常下载（如 PDF），交给系统 DownloadManager（保持用户常规文件下载能力）。
 *
 * 拦截策略（shouldOverrideUrlLoading + setDownloadListener 双保险）：
 *   - shouldOverrideUrlLoading：拦截 URL 跳转。若 URL 以 .xml / .mxl / .musicxml 结尾，
 *     直接拦下交给 OnlineScoreDownloader 下载 → 自动打开。
 *   - setDownloadListener：应对服务器不直接跳转，而是返回 Content-Disposition 的情况。
 *     同样根据 url / mimetype 判断是否为 MusicXML；如是，则复用 OnlineScoreDownloader 下载器；
 *     否则交给 DownloadManager。
 */
public class PlatformWebViewActivity extends AppCompatActivity {
    public static final String EXTRA_INIT_URL = "init_url";
    public static final String EXTRA_PLATFORM_NAME = "platform_name";

    private static final int REQ_WRITE = 1001;
    private static final String FILEPROVIDER_AUTH = "com.rojama.pianoshelf.fileprovider";

    private WebView webView;
    private ProgressBar progressTop;
    private ProgressBar progressCenter;
    private String pendingUrlToDl; // 等待权限授权后再下载

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.platform_webview);

        webView = findViewById(R.id.web_view);
        progressTop = findViewById(R.id.progress_web);
        progressCenter = findViewById(R.id.progress_center);

        String initUrl = getIntent().getStringExtra(EXTRA_INIT_URL);
        String name = getIntent().getStringExtra(EXTRA_PLATFORM_NAME);
        if (TextUtils.isEmpty(initUrl)) initUrl = "https://musescore.com/sheetmusic";
        if (name == null) name = "";

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(name.isEmpty() ? getString(R.string.webview_default_title) : name);
        }

        setupWebView();
        webView.loadUrl(initUrl);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            try {
                webView.stopLoading();
                webView.removeAllViews();
                webView.destroy();
            } catch (Throwable ignored) {}
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    // ------------------------------------------------------------------
    // WebView 配置
    // ------------------------------------------------------------------

    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);              // 现代乐谱站几乎都需 JS
        s.setDomStorageEnabled(true);               // MuseScore SPA 依赖
        s.setDatabaseEnabled(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setUserAgentString(s.getUserAgentString()
                + " PianoShelf-Android/2.0");

        // 允许第三方 Cookies（MuseScore 需要登录 Cookie）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }
        CookieManager.getInstance().setAcceptCookie(true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (progressTop == null) return;
                progressTop.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
                progressTop.setProgress(newProgress);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            // ---- URL 拦截：若为 MusicXML 直链，直接交给下载器并打开 ----
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String url = uri == null ? "" : uri.toString();
                return interceptIfMusicXml(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return interceptIfMusicXml(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (progressCenter != null) progressCenter.setVisibility(View.GONE);
            }
        });

        // ---- 文件下载监听（针对触发 "下载" 按钮而非跳转链接的情况）----
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype, long contentLength) {
                if (isMusicXmlTarget(url, mimetype, contentDisposition)) {
                    startAppInternalDownload(url);
                } else {
                    // 正常非 MusicXML 文件：交给系统 DownloadManager（若有权限）
                    downloadViaSystem(url, userAgent, contentDisposition, mimetype);
                }
            }
        });

        // 初始加载显示中心进度
        progressCenter.setVisibility(View.VISIBLE);
    }

    // ------------------------------------------------------------------
    // 拦截 & 下载分发
    // ------------------------------------------------------------------

    /**
     * 判断 URL 是否为 MusicXML 资源（命中则拦截，返回 true）。
     * 命中后交给 OnlineScoreDownloader，并启动 GraphicsActivity。
     */
    private boolean interceptIfMusicXml(String url) {
        if (TextUtils.isEmpty(url)) return false;
        String low = url.toLowerCase();
        if (!(low.startsWith("http://") || low.startsWith("https://"))) return false;
        // 先从 URL 判断后缀（含 .gz 压缩版）
        int q = low.indexOf('?');
        String path = (q >= 0) ? low.substring(0, q) : low;
        boolean hit = false;
        for (String ext : new String[]{".xml", ".mxl", ".musicxml"}) {
            if (path.endsWith(ext) || path.endsWith(ext + ".gz")) { hit = true; break; }
        }
        if (hit) {
            startAppInternalDownload(url);
            return true;
        }
        // 若未命中，WebView 继续加载
        return false;
    }

    private static boolean isMusicXmlTarget(String url, String mimetype, String cd) {
        if (!TextUtils.isEmpty(url)) {
            String low = url.toLowerCase();
            int q = low.indexOf('?');
            String p = (q >= 0) ? low.substring(0, q) : low;
            for (String ext : new String[]{".xml", ".mxl", ".musicxml"}) {
                if (p.endsWith(ext) || p.endsWith(ext + ".gz")) return true;
            }
        }
        if (mimetype != null) {
            String m = mimetype.toLowerCase();
            if (m.contains("musicxml")) return true;
            if (m.equals("application/xml") || m.equals("text/xml")) {
                // 仅凭 application/xml 不足以判定，再看 Content-Disposition 文件名
                if (cd != null) {
                    String low = cd.toLowerCase();
                    if (low.contains(".musicxml") || low.contains(".mxl")) return true;
                }
            }
        }
        return false;
    }

    // ---- App 内部下载 + 自动打开（用 OnlineScoreDownloader 缓存体系）----
    private void startAppInternalDownload(final String url) {
        String valid = OnlineScoreDownloader.validateUrl(url);
        // 因为 .xml.gz 合法，validateUrl 目前只允许 .xml/.mxl/.musicxml；这里放宽为 gz 压缩版也是可接受的
        // 若 validateUrl 不通过，则尝试给 URL 临时去 .gz 尾判断 / 或强制信任拦截命中
        if (valid == null) {
            launchDownloadInternal(url);
            return;
        }
        // .xml.gz 走这里：我们已经在 interceptIfMusicXml 手动确认后缀，直接放行
        String low = url.toLowerCase();
        if (low.endsWith(".xml.gz") || low.endsWith(".musicxml.gz")) {
            launchDownloadInternal(url);
            return;
        }
        Toast.makeText(this, valid, Toast.LENGTH_LONG).show();
    }

    private void launchDownloadInternal(String url) {
        Toast.makeText(this, R.string.webview_intercepted_dl, Toast.LENGTH_SHORT).show();
        final String[] localHolder = new String[1];
        OnlineScoreDownloader.downloadAsync(this, url, false, new OnlineScoreDownloader.DownloadCallback() {
            @Override public void onProgress(int percent) {}
            @Override public void onSuccess(File localFile) {
                localHolder[0] = localFile.getAbsolutePath();
                // 写入最近记录 & 启动打开
                DatabaseHelper dbh = new DatabaseHelper(PlatformWebViewActivity.this);
                dbh.insertRecentItem(localFile.getAbsolutePath());
                try { dbh.close(); } catch (Throwable ignored) {}
                try {
                    Uri uri = FileProvider.getUriForFile(PlatformWebViewActivity.this,
                            FILEPROVIDER_AUTH, localFile);
                    Intent intent = new Intent();
                    intent.setDataAndType(uri, "application/vnd.recordare.musicxml");
                    intent.setClass(PlatformWebViewActivity.this, GraphicsActivity.class);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.putExtra(GraphicsActivity.EXTRA_FILE_PATH, localFile.getAbsolutePath());
                    startActivity(intent);
                    Toast.makeText(PlatformWebViewActivity.this,
                            R.string.online_open_ok, Toast.LENGTH_SHORT).show();
                } catch (Throwable t) {
                    Toast.makeText(PlatformWebViewActivity.this,
                            getString(R.string.info_open_err) + " " + t.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(String message) {
                Toast.makeText(PlatformWebViewActivity.this,
                        getString(R.string.online_status_fail) + "\n" + message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // ---- 非 MusicXML 文件：系统 DownloadManager ----
    private void downloadViaSystem(String url, String ua, String cd, String mime) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 不需要 WRITE_EXTERNAL_STORAGE 即可写公共下载目录
            doDownloadViaSystem(url, ua, cd, mime);
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            doDownloadViaSystem(url, ua, cd, mime);
        } else {
            pendingUrlToDl = url + "|||" + ua + "|||" + (cd == null ? "" : cd) + "|||" + (mime == null ? "" : mime);
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_WRITE);
        }
    }

    private void doDownloadViaSystem(String url, String ua, String cd, String mime) {
        try {
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
            String name = URLUtil.guessFileName(url, cd, mime);
            req.setTitle(name);
            req.setDescription(getString(R.string.webview_sysdl_desc));
            req.setMimeType(mime);
            req.allowScanningByMediaScanner();
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
            req.addRequestHeader("User-Agent", ua == null ? webView.getSettings().getUserAgentString() : ua);
            // 复制当前 Cookie（MuseScore 需要登录才能下载）
            String cookie = CookieManager.getInstance().getCookie(url);
            if (!TextUtils.isEmpty(cookie)) req.addRequestHeader("Cookie", cookie);
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (dm != null) {
                long id = dm.enqueue(req);
                watchSystemDownload(id, name);
                Toast.makeText(this, getString(R.string.webview_sysdl_starting, name), Toast.LENGTH_SHORT).show();
            }
        } catch (Throwable t) {
            Toast.makeText(this, getString(R.string.webview_sysdl_fail) + " " + t.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 监听系统 DownloadManager 完成 → 若下载的文件是 MusicXML（用户在 WebView 中下载 .xml），
     * 自动弹出 "打开"。简单起见：注册一个一次性广播，完成后立即 unregister。
     */
    private void watchSystemDownload(final long id, final String name) {
        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (intent == null) return;
                long id2 = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id2 != id) return;
                try { unregisterReceiver(this); } catch (Throwable ignored) {}
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (dm == null) return;
                Cursor c = dm.query(new DownloadManager.Query().setFilterById(id));
                if (c == null) return;
                try {
                    if (!c.moveToFirst()) return;
                    int st = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    if (st != DownloadManager.STATUS_SUCCESSFUL) return;
                    String localUri = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
                    if (TextUtils.isEmpty(localUri)) return;
                    // 尝试解析本地路径
                    File f;
                    try { f = new File(Uri.parse(localUri).getPath()); }
                    catch (Throwable t) { f = new File(localUri.replaceFirst("^file://", "")); }
                    if (!f.exists()) return;
                    String low = f.getName().toLowerCase();
                    if (low.endsWith(".xml") || low.endsWith(".mxl") || low.endsWith(".musicxml") || low.endsWith(".xml.gz")) {
                        // 也是乐谱 → 直接打开
                        DatabaseHelper dbh = new DatabaseHelper(PlatformWebViewActivity.this);
                        dbh.insertRecentItem(f.getAbsolutePath());
                        try { dbh.close(); } catch (Throwable ignored) {}
                        try {
                            Uri u = FileProvider.getUriForFile(PlatformWebViewActivity.this,
                                    FILEPROVIDER_AUTH, f);
                            Intent i = new Intent();
                            i.setDataAndType(u, "application/vnd.recordare.musicxml");
                            i.setClass(PlatformWebViewActivity.this, GraphicsActivity.class);
                            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            i.putExtra(GraphicsActivity.EXTRA_FILE_PATH, f.getAbsolutePath());
                            startActivity(i);
                        } catch (Throwable ignored) {}
                    }
                } finally {
                    try { c.close(); } catch (Throwable ignored) {}
                }
            }
        };
        try {
            registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        } catch (Throwable ignored) {}
    }

    // ------------------------------------------------------------------
    // 运行时权限回调（低版本需要 WRITE 下载到公共目录）
    // ------------------------------------------------------------------

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_WRITE && pendingUrlToDl != null) {
            String[] split = pendingUrlToDl.split("\\|\\|\\|", -1);
            pendingUrlToDl = null;
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                doDownloadViaSystem(
                        split.length > 0 ? split[0] : "",
                        split.length > 1 ? split[1] : null,
                        split.length > 2 ? split[2] : null,
                        split.length > 3 ? split[3] : null);
            } else {
                Toast.makeText(this, R.string.webview_perm_denied, Toast.LENGTH_LONG).show();
            }
        }
    }
}
