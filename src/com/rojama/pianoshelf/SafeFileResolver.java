package com.rojama.pianoshelf;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 跨 Scoped Storage / 旧 File / SAF / MediaStore 的 URI → 本地可读 File 的归一化工具。
 *
 * 核心设计：
 *  - 不假设任何 URI 是"本地路径"，所有传入 content:// / file:// / android.resource
 *    甚至 http(s)://（极端情况）都先转成 cache dir 里一份稳定副本。
 *  - 副本按 SHA-256(uri) + 扩展名命名，避免重复拷贝。
 *  - 所有流操作强制 try-with-resources + 8KB 缓冲，避免 OEM ContentProvider 读到一半
 *    死锁/断流时泄漏 fd。
 */
public final class SafeFileResolver {
    private static final int BUF = 8192;
    private static final String CACHE_SUBDIR = "imported_scores";

    private SafeFileResolver() {}

    /**
     * @return 归一化后的本地绝对路径；出错返回 null（由调用方决定 UI 提示）。
     */
    @Nullable
    public static String materializeToCacheFile(@NonNull Context ctx, @NonNull Uri uri) {
        if (uri.getPath() == null) return null;
        // Fast path: 纯 file:// 且目标可直接读 → 直接返回绝对路径。
        String scheme = uri.getScheme();
        if ((scheme == null || "file".equalsIgnoreCase(scheme))) {
            String path = uri.getPath();
            if (path != null) {
                File f = new File(path);
                if (f.isFile() && f.canRead()) return f.getAbsolutePath();
            }
        }
        // Slow path: 把 InputStream 复制到 cache dir.
        try {
            String ext = extractExtension(uri);
            File dir = ensureCacheDir(ctx);
            String hash = sha256Hex(uri.toString());
            File target = new File(dir, hash + ext);
            if (target.isFile() && target.length() > 0) return target.getAbsolutePath();
            ContentResolver cr = ctx.getContentResolver();
            try (InputStream in = openInputStreamBestEffort(cr, uri)) {
                if (in == null) return null;
                File tmp = new File(dir, hash + ".part");
                try (OutputStream out = new FileOutputStream(tmp)) {
                    byte[] b = new byte[BUF];
                    int n;
                    while ((n = in.read(b)) > 0) out.write(b, 0, n);
                }
                if (!tmp.renameTo(target)) {
                    // 跨卷 rename 失败时 fallback copy-delete
                    copyFile(tmp, target);
                    tmp.delete();
                }
            }
            return target.getAbsolutePath();
        } catch (Throwable t) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // DocumentsContract (SAF 目录树) 辅助：
    //   给定 treeUri + docId，列出子 doc；给定 doc 读取文件 → 走 materializeToCacheFile
    // ------------------------------------------------------------------

    public static final class DocEntry {
        public final String docId;
        public final String displayName;
        public final String mimeType;
        public final long   size;
        public final boolean isDir;
        public DocEntry(String docId, String displayName, String mimeType, long size, boolean isDir) {
            this.docId = docId; this.displayName = displayName;
            this.mimeType = mimeType; this.size = size; this.isDir = isDir;
        }
    }

    public static boolean isTreeUriSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static String getRootDocId(@NonNull Uri treeUri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return null;
        return DocumentsContract.getTreeDocumentId(treeUri);
    }

    public static Uri buildChildUri(@NonNull Uri treeUri, @NonNull String docId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return null;
        return DocumentsContract.buildDocumentUriUsingTree(treeUri, docId);
    }

    @NonNull
    public static java.util.ArrayList<DocEntry> listTreeChildren(@NonNull Context ctx,
            @NonNull Uri treeUri, @Nullable String parentDocId) {
        java.util.ArrayList<DocEntry> out = new java.util.ArrayList<>();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return out;
        try {
            String docId = (parentDocId != null) ? parentDocId : getRootDocId(treeUri);
            if (docId == null) return out;
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId);
            String[] proj = {
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_FLAGS
            };
            try (Cursor c = ctx.getContentResolver().query(childrenUri, proj, null, null, null)) {
                if (c == null) return out;
                int cId = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
                int cName = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
                int cMime = c.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE);
                int cSize = c.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE);
                int cFlags = c.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS);
                while (c.moveToNext()) {
                    String id = c.isNull(cId) ? null : c.getString(cId);
                    String name = c.isNull(cName) ? null : c.getString(cName);
                    String mime = c.isNull(cMime) ? null : c.getString(cMime);
                    long sz = c.isNull(cSize) ? 0L : c.getLong(cSize);
                    int flags = c.isNull(cFlags) ? 0 : c.getInt(cFlags);
                    boolean dir = DocumentsContract.Document.MIME_TYPE_DIR.equals(mime);
                    if (id == null || TextUtils.isEmpty(name)) continue;
                    // 仅保留目录或 MusicXML（快速过滤，避免列目录时把 10000 张图片都列出来）
                    if (!dir) {
                        String low = name.toLowerCase();
                        if (!(low.endsWith(".xml") || low.endsWith(".mxl") || low.endsWith(".musicxml")
                                || (mime != null && mime.toLowerCase().contains("xml")))) continue;
                    }
                    out.add(new DocEntry(id, name, mime, sz, dir));
                }
            }
        } catch (Throwable ignore) {
            // Provider 被用户取消 / USB 断开时直接返回空
        }
        return out;
    }

    public static boolean isValidMusicXmlName(@Nullable String name) {
        if (name == null) return false;
        String low = name.toLowerCase();
        return low.endsWith(".xml") || low.endsWith(".mxl") || low.endsWith(".musicxml");
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private static File ensureCacheDir(Context ctx) throws IOException {
        File d = new File(ctx.getCacheDir(), CACHE_SUBDIR);
        if (!d.exists() && !d.mkdirs() && !d.isDirectory()) {
            throw new IOException("Cannot create cache dir: " + d);
        }
        return d;
    }

    private static String extractExtension(Uri uri) {
        // 先看 displayName；ContentResolver.query(uri, [DISPLAY_NAME]) 最准
        try {
            Cursor c = null;
            try {
                c = App.getContextForResolver().getContentResolver()
                        .query(uri, new String[]{ OpenableColumns.DISPLAY_NAME },
                                null, null, null);
                if (c != null && c.moveToFirst()) {
                    String n = c.getString(0);
                    if (n != null) {
                        int i = n.lastIndexOf('.');
                        if (i >= 0) return n.substring(i).toLowerCase();
                    }
                }
            } finally { if (c != null) c.close(); }
        } catch (Throwable ignore) {}
        // fallback: parse from uri last path segment
        String p = uri.getLastPathSegment();
        if (p != null) {
            int i = p.lastIndexOf('.');
            if (i >= 0) return p.substring(i).toLowerCase();
        }
        return ".tmp";
    }

    private static InputStream openInputStreamBestEffort(ContentResolver cr, Uri uri) {
        try {
            // 对 treeUri 下的 document 也可以直接开流
            return cr.openInputStream(uri);
        } catch (Throwable t) { return null; }
    }

    private static void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new java.io.FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] b = new byte[BUF]; int n;
            while ((n = in.read(b)) > 0) out.write(b, 0, n);
        }
    }

    private static void closeQuietly(Closeable c) {
        try { if (c != null) c.close(); } catch (Throwable ignore) {}
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes());
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to stable hashCode hex (best effort).
            return "fallback_" + Integer.toHexString(s.hashCode());
        }
    }

    /** Tiny helper to get a ContentResolver-capable Context without import cycles. */
    private static final class App {
        static Context getContextForResolver() {
            // PianoShelfApp (if exists later) can return ctx; fallback uses global Application via
            // android.app.ActivityThread.currentApplication() reflection-free pattern via
            // androidx.core.content.ContextCompat:
            // We just return a global app-level Context obtained from
            // PianoShelfApp via null-safe reflection chain.
            try {
                Class<?> at = Class.forName("android.app.ActivityThread");
                java.lang.reflect.Method m = at.getMethod("currentApplication");
                Object app = m.invoke(null);
                if (app instanceof Context) return (Context) app;
            } catch (Throwable ignore) {}
            throw new RuntimeException("No application Context available");
        }
    }
}
