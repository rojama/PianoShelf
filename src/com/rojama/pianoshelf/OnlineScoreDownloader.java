package com.rojama.pianoshelf;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 在线 MusicXML 乐谱下载工具类。
 *
 * 特性：
 *  - 基于 OkHttp 4.x，自带连接池 + gzip 解压 + 超时控制
 *  - 文件 URL 校验（仅允许 http/https，校验后缀 .xml / .mxl / .musicxml）
 *  - 磁盘缓存：已下载的文件用 SHA-256(url) 作为文件名，避免重复下载
 *  - 下载回调：运行在主线程，方便直接更新 UI
 *
 * 支持的免费在线平台（用户在这些平台找到乐谱后复制链接即可）：
 *  - MuseScore.com (公有领域免费 MusicXML)
 *  - IMSLP.org (经典公有领域乐谱，部分包含 MusicXML 附件)
 *  - MutopiaProject.org (LilyPond 高质量排版，MusicXML/PDF)
 *  - 以及任意直接可下载的 .xml / .mxl URL
 */
public class OnlineScoreDownloader {
    private static final String TAG = "OnlineScoreDownloader";

    private static final int CONNECT_TIMEOUT_S = 15;
    private static final int READ_TIMEOUT_S = 60;
    private static final int BUFFER_SIZE = 8192;

    // 允许的文件扩展名（不区分大小写）
    private static final String[] ALLOWED_EXTS = {".xml", ".mxl", ".musicxml"};

    // 子缓存目录名 (位于 app cache dir)
    private static final String CACHE_DIR = "online_scores";

    // 预置平台推荐信息
    public static final List<PlatformInfo> PRESET_PLATFORMS;

    public static class PlatformInfo {
        public final String name;
        public final String description;
        public final String websiteUrl;

        public PlatformInfo(String name, String description, String websiteUrl) {
            this.name = name;
            this.description = description;
            this.websiteUrl = websiteUrl;
        }
    }

    static {
        List<PlatformInfo> list = new ArrayList<>();
        list.add(new PlatformInfo(
                "MuseScore",
                "全球最大乐谱社区，100万+乐谱，公有领域可免费下载 MusicXML",
                "https://musescore.com/sheetmusic"
        ));
        list.add(new PlatformInfo(
                "IMSLP",
                "国际乐谱图书馆，18万+公有领域经典乐谱（巴赫/贝多芬/莫扎特等）",
                "https://imslp.org/wiki/Main_Page"
        ));
        list.add(new PlatformInfo(
                "Mutopia Project",
                "LilyPond 高质量排版，古典乐公共领域，MusicXML/PDF 双格式",
                "http://www.mutopiaproject.org/"
        ));
        list.add(new PlatformInfo(
                "OpenScore (MuseScore)",
                "全球公有领域乐谱交互式项目，自由免费使用",
                "https://musescore.com/openscore"
        ));
        PRESET_PLATFORMS = Collections.unmodifiableList(list);
    }

    // ------------------------------------------------------------------
    // Download callback (always delivered on main thread)
    // ------------------------------------------------------------------

    public interface DownloadCallback {
        /**
         * 下载进度。
         * @param percent 0~100，-1 表示服务器没返回 Content-Length（无法计算）
         */
        void onProgress(int percent);

        /** 下载成功，返回本地缓存文件绝对路径。 */
        void onSuccess(File localFile);

        /** 下载失败，message 为可读错误说明。 */
        void onFailure(String message);
    }

    // ------------------------------------------------------------------
    // Singleton OkHttp client (reuse connection pool)
    // ------------------------------------------------------------------

    private static volatile OkHttpClient client;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static OkHttpClient client() {
        if (client == null) {
            synchronized (OnlineScoreDownloader.class) {
                if (client == null) {
                    client = new OkHttpClient.Builder()
                            .connectTimeout(CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
                            .readTimeout(READ_TIMEOUT_S, TimeUnit.SECONDS)
                            .followRedirects(true)
                            .followSslRedirects(true)
                            .build();
                }
            }
        }
        return client;
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * 校验 URL 是否为可用的在线乐谱地址。
     * @return null 表示有效，否则返回错误原因（英文资源 ID 通过 Context 取更合适；
     *         这里直接返回中文错误文本，调用方也可自行翻译）。
     */
    public static String validateUrl(String url) {
        if (TextUtils.isEmpty(url)) return "URL 不能为空";
        String lower = url.trim().toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return "仅支持 http 或 https 链接";
        }
        // 允许带 query string，所以判断 "路径部分" 的后缀
        int q = lower.indexOf('?');
        String pathPart = (q >= 0) ? lower.substring(0, q) : lower;
        for (String ext : ALLOWED_EXTS) {
            if (pathPart.endsWith(ext)) return null;
        }
        return "链接必须以 .xml / .mxl / .musicxml 结尾";
    }

    /**
     * 获取缓存目录（不存在则自动创建）。
     */
    public static File getCacheDir(Context context) throws IOException {
        File dir = new File(context.getCacheDir(), CACHE_DIR);
        if (!dir.exists()) {
            if (!dir.mkdirs() && !dir.isDirectory()) {
                throw new IOException("Cannot create cache dir: " + dir);
            }
        }
        return dir;
    }

    /**
     * 根据 URL 计算本地缓存文件。
     *   策略：SHA-256(url).hex + URL 中解析出的扩展名，
     *   这样相同 URL 不会重复下载，且扩展名保留方便后续识别。
     */
    public static File cachedFileFor(Context context, String url) throws IOException {
        String lower = url.trim().toLowerCase();
        int q = lower.indexOf('?');
        String pathPart = (q >= 0) ? lower.substring(0, q) : lower;
        String ext = "";
        for (String allowed : ALLOWED_EXTS) {
            if (pathPart.endsWith(allowed)) { ext = allowed; break; }
        }
        String hash = sha256Hex(url.trim());
        return new File(getCacheDir(context), hash + ext);
    }

    /**
     * 启动异步下载。回调在主线程触发。
     *
     * @param force 若本地已有缓存是否强制重新下载
     */
    public static void downloadAsync(final Context context,
                                     final String url,
                                     final boolean force,
                                     final DownloadCallback callback) {
        final String err = validateUrl(url);
        if (err != null) {
            postFail(callback, err);
            return;
        }

        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    File cache = cachedFileFor(context, url);
                    if (cache.exists() && cache.length() > 0 && !force) {
                        postOk(callback, cache);
                        return;
                    }
                    doDownload(url, cache, callback);
                } catch (Throwable t) {
                    Log.e(TAG, "download failed", t);
                    postFail(callback, "下载失败: " + (t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage()));
                }
            }
        }, "PianoShelf-Downloader").start();
    }

    /** 清理所有缓存文件。返回清理释放的字节数。 */
    public static long clearCache(Context context) {
        try {
            File dir = getCacheDir(context);
            long freed = 0L;
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) {
                        long len = f.length();
                        if (f.delete()) freed += len;
                    }
                }
            }
            return freed;
        } catch (IOException ignored) {
            return 0L;
        }
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private static void doDownload(String url, File target, DownloadCallback callback) throws IOException {
        Request request = new Request.Builder().url(url).get().build();
        try (Response response = client().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                postFail(callback, "服务器返回错误: HTTP " + response.code());
                return;
            }
            ResponseBody body = response.body();
            if (body == null) {
                postFail(callback, "响应体为空");
                return;
            }
            long contentLength = body.contentLength(); // -1 if unknown
            InputStream in = body.byteStream();
            File tmp = new File(target.getParentFile(), target.getName() + ".part");
            long written = 0L;
            int lastPercent = -1;
            try (FileOutputStream fos = new FileOutputStream(tmp)) {
                byte[] buf = new byte[BUFFER_SIZE];
                int n;
                while ((n = in.read(buf)) != -1) {
                    fos.write(buf, 0, n);
                    written += n;
                    if (contentLength > 0) {
                        int p = (int) (written * 100 / contentLength);
                        if (p != lastPercent) {
                            lastPercent = p;
                            postProgress(callback, p);
                        }
                    } else {
                        postProgress(callback, -1);
                    }
                }
                fos.getFD().sync();
            }
            if (!tmp.renameTo(target)) {
                // 某些系统 rename 失败，回退到 copy+delete
                copyFile(tmp, target);
                //noinspection ResultOfMethodCallIgnored
                tmp.delete();
            }
            postProgress(callback, 100);
            postOk(callback, target);
        }
    }

    private static void copyFile(File src, File dst) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(dst);
             java.io.FileInputStream fis = new java.io.FileInputStream(src)) {
            byte[] buf = new byte[BUFFER_SIZE];
            int n;
            while ((n = fis.read(buf)) != -1) fos.write(buf, 0, n);
            fos.getFD().sync();
        }
    }

    // ------------------------------------------------------------------
    // Callback dispatch (switch to main thread)
    // ------------------------------------------------------------------

    private static void postProgress(final DownloadCallback cb, final int p) {
        if (cb == null) return;
        mainHandler.post(new Runnable() { @Override public void run() { cb.onProgress(p); } });
    }

    private static void postOk(final DownloadCallback cb, final File f) {
        if (cb == null) return;
        mainHandler.post(new Runnable() { @Override public void run() { cb.onSuccess(f); } });
    }

    private static void postFail(final DownloadCallback cb, final String m) {
        if (cb == null) return;
        mainHandler.post(new Runnable() { @Override public void run() { cb.onFailure(m); } });
    }

    // ------------------------------------------------------------------
    // Crypto helper: SHA-256 -> hex
    // ------------------------------------------------------------------

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes());
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                int v = b & 0xFF;
                if (v < 16) sb.append('0');
                sb.append(Integer.toHexString(v));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // 所有 Android 平台都支持 SHA-256，此分支极端情况下退化到 hashCode
            return Integer.toHexString(s.hashCode());
        }
    }
}
