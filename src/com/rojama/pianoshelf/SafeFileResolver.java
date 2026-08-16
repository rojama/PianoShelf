package com.rojama.pianoshelf;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SAF / ContentResolver URI → 本地缓存文件的归一化工具。
 *
 * 使用场景：当用户通过 ACTION_OPEN_DOCUMENT 选到一个 MusicXML/MXL 文件，
 * 拿到的是 content:// URI（不能直接按 String path 传进 GraphicsActivity）。
 * 解决办法：复制成 app 私有 cache 目录下的一个真实文件，返回其绝对路径。
 * （GraphicsActivity 通过 FileProvider 再读取这个文件时已可正常解析。）
 *
 * 文件名：sha256(uri.toString()) + 扩展名（若 DISPLAY_NAME 可解析则附带原扩展名）。
 * 相同 URI 重复选择时命中已有文件，避免重复复制。
 */
public final class SafeFileResolver {
	private SafeFileResolver() {}

	/**
	 * 把 content:// Uri 复制到 app cache 并返回绝对路径。
	 * @return 成功返回绝对路径字符串；失败返回 null（调用方弹提示）。
	 */
	@Nullable
	public static String materializeToCacheFile(@NonNull Context context, @NonNull Uri uri) {
		DebugLog.ensureInitialized(context);
		try {
			DebugLog.i("SafeResolver", "materializeToCacheFile  uri=" + uri + " scheme=" + uri.getScheme()
					+ " mime=" + context.getContentResolver().getType(uri));
			String ext = extractExtension(context, uri);
			String filenameHash = sha256(uri.toString());
			String cacheName = "p_" + filenameHash + (ext.isEmpty() ? "" : "." + ext);
			File out = new File(context.getCacheDir(), cacheName);
			DebugLog.d("SafeResolver", "  ext=" + ext + "  cachePath=" + out
					+ "  已存在且非空=" + (out.exists() && out.length() > 0));

			if (!out.exists() || out.length() == 0) {
				InputStream in = null;
				OutputStream os = null;
				long copied = 0;
				long t0 = System.currentTimeMillis();
				try {
					in = context.getContentResolver().openInputStream(uri);
					DebugLog.d("SafeResolver", "  打开 ContentResolver.openInputStream => " + (in == null ? "null" : in.getClass()));
					if (in == null) {
						DebugLog.w("SafeResolver", "  openInputStream 返回 null");
						return null;
					}
					os = new FileOutputStream(out);
					copied = copyStream(in, os);
				} finally {
					closeQuiet(in);
					closeQuiet(os);
				}
				long t1 = System.currentTimeMillis();
				DebugLog.i("SafeResolver", "  复制完成  bytes=" + copied + "  耗时=" + (t1 - t0) + "ms");
			}
			boolean ok = out.exists() && out.length() > 0;
			DebugLog.i("SafeResolver", "  final: out.exists=" + out.exists()
					+ " size=" + (out.exists() ? out.length() : -1)
					+ " => return " + (ok ? out.getAbsolutePath() : "null"));
			return ok ? out.getAbsolutePath() : null;
		} catch (Throwable t) {
			DebugLog.e("SafeResolver", "materializeToCacheFile 异常", t);
			return null;
		}
	}

	private static String extractExtension(@NonNull Context context, @NonNull Uri uri) {
		String name = null;
		Cursor c = null;
		try {
			c = context.getContentResolver().query(
					uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
			if (c != null && c.moveToFirst()) {
				int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
				if (idx >= 0) name = c.getString(idx);
			}
		} catch (Throwable ignore) {
		} finally {
			if (c != null) try { c.close(); } catch (Throwable ignore) {}
		}
		if (name == null) name = uri.getLastPathSegment();
		DebugLog.d("SafeResolver", "  extractExtension => DISPLAY_NAME/lastSegment=" + name);
		if (name == null) return "";
		int dot = name.lastIndexOf('.');
		if (dot < 0 || dot == name.length() - 1) return "";
		String ext = name.substring(dot + 1);
		DebugLog.d("SafeResolver", "    extension=" + ext);
		return ext;
	}

	private static long copyStream(@NonNull InputStream in, @NonNull OutputStream out) throws IOException {
		byte[] buf = new byte[8192];
		int r;
		long total = 0;
		while ((r = in.read(buf)) > 0) {
			out.write(buf, 0, r);
			total += r;
		}
		out.flush();
		return total;
	}

	private static void closeQuiet(@Nullable Object o) {
		if (o instanceof InputStream) try { ((InputStream) o).close(); } catch (Throwable ignore) {}
		else if (o instanceof OutputStream) try { ((OutputStream) o).close(); } catch (Throwable ignore) {}
		else if (o instanceof Cursor) try { ((Cursor) o).close(); } catch (Throwable ignore) {}
		else if (o instanceof ParcelFileDescriptor) try { ((ParcelFileDescriptor) o).close(); } catch (Throwable ignore) {}
	}

	@NonNull
	private static String sha256(@NonNull String s) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] d = md.digest(s.getBytes());
			StringBuilder sb = new StringBuilder(d.length * 2);
			for (byte b : d) sb.append(String.format("%02x", b & 0xff));
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			return Integer.toHexString(s.hashCode());
		}
	}
}
