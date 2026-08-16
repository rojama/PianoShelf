package com.rojama.pianoshelf;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * 强可靠调试日志：
 *  1) 环形内存 buffer（200 行）+ UI 订阅（主线程回调）
 *  2) 同时写入 /sdcard/Android/data/<pkg>/files/debug.log（应用级存储，不需要任何权限）
 *  3) 所有日志都用 Log.println(ASSERT) 写到 logcat (保证在 release + 厂商关闭 debug 开关时也不会被吃掉)
 *
 *  为什么用 ASSERT / Log.wtf 级别：
 *    - MIUI/HyperOS 默认对第三方 App 做 logcat 节流，Log.d/i/w 很容易被系统消音
 *    - ASSERT 级只在 App 崩溃时才见，但这里我们不抛异常、仅用作 "强制留痕" 级别，
 *      logcat 里搜 tag 就能看到。
 */
public final class DebugLog {
    public static final int MAX_LINES = 500;

    public enum Level { V, D, I, W, E }

    public interface Listener {
        /** deltaOrAll 可能是单条，也可能是订阅时的整份历史（一段大字符串）。 */
        void onAppended(String deltaOrAll);
    }

    private static final Object LOCK = new Object();
    private static final ArrayDeque<String> RING = new ArrayDeque<>(MAX_LINES + 16);
    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
    private static final SimpleDateFormat FILE_SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static Listener listener;

    private static volatile Context appCtx;
    private static volatile File logFile;
    private static volatile BufferedOutputStream logStream;
    private static volatile boolean initDone = false;

    /** 静态启动标记：类加载时先放一条，确保在任何 Activity/Application.onCreate 之前 ring 里都有第一条。 */
    static {
        rawRingAppend(String.format(Locale.US,
                "[BOOT] %s  DebugLog class-loaded (before Application.onCreate)",
                SDF.format(new Date())));
    }

    private DebugLog() {}

    public static void ensureInitialized(Context ctx) {
        if (initDone) return;
        synchronized (LOCK) {
            if (initDone) return;
            try {
                appCtx = ctx.getApplicationContext();
                File dir = appCtx.getExternalFilesDir(null);
                if (dir == null) dir = appCtx.getFilesDir();
                if (dir != null) {
                    logFile = new File(dir, "debug.log");
                    // 每次启动重开一个：把前一次的尾 20KB 保留当历史，其余清空，避免占满存储空间
                    try { rollLog(logFile); } catch (Throwable ignore) {}
                    logStream = new BufferedOutputStream(new FileOutputStream(logFile, true), 8192);
                    rawRingAppend("[LOG] 持久化日志文件：" + logFile);
                    writeToFileLocked(String.format(Locale.US,
                            "\n===== session start %s =====\n",
                            FILE_SDF.format(new Date())));
                }
            } catch (Throwable t) {
                // 持久化失败也不影响 UI 输出
                try { rawRingAppend("[LOG] 持久化日志 init 失败：" + t); } catch (Throwable ignore) {}
            } finally {
                initDone = true;
            }
        }
    }

    public static String getLogFilePath() {
        File f = logFile;
        return f == null ? null : f.getAbsolutePath();
    }

    private static void rollLog(File f) {
        if (f == null || !f.isFile()) return;
        long len = f.length();
        if (len <= 20 * 1024) return;
        File tmp = new File(f.getAbsolutePath() + ".bak");
        try {
            byte[] tail = new byte[20 * 1024];
            java.io.RandomAccessFile raf = new java.io.RandomAccessFile(f, "r");
            try {
                raf.seek(len - tail.length);
                int got = raf.read(tail);
                raf.close();
                FileOutputStream fos = new FileOutputStream(f);
                try {
                    fos.write(tail, 0, Math.max(0, got));
                    fos.write(("\n---- trimmed previous " + len + " bytes ----\n").getBytes());
                } finally {
                    try { fos.close(); } catch (Throwable ignore) {}
                }
            } finally {
                try { raf.close(); } catch (Throwable ignore) {}
            }
            try { tmp.delete(); } catch (Throwable ignore) {}
        } catch (Throwable ignore) {}
    }

    /** UI 订阅：立刻 MAIN.post 回调整份历史，之后每行 append 也 MAIN.post 回调 delta。 */
    public static void setListener(Listener l) {
        synchronized (LOCK) {
            listener = l;
            if (l != null) {
                final List<String> snap = new ArrayList<>(RING);
                final Listener copy = l;
                MAIN.post(() -> {
                    StringBuilder sb = new StringBuilder(4096);
                    for (String s : snap) sb.append(s).append('\n');
                    copy.onAppended(sb.toString());
                });
            }
        }
    }

    public static String dumpAll() {
        synchronized (LOCK) {
            StringBuilder sb = new StringBuilder(4096);
            for (String s : RING) sb.append(s).append('\n');
            return sb.toString();
        }
    }

    public static void v(String tag, String msg) { log(Level.V, tag, msg, null); }
    public static void d(String tag, String msg) { log(Level.D, tag, msg, null); }
    public static void i(String tag, String msg) { log(Level.I, tag, msg, null); }
    public static void w(String tag, String msg) { log(Level.W, tag, msg, null); }
    public static void w(String tag, String msg, Throwable t) { log(Level.W, tag, msg, t); }
    public static void e(String tag, String msg) { log(Level.E, tag, msg, null); }
    public static void e(String tag, String msg, Throwable t) { log(Level.E, tag, msg, t); }

    private static void log(Level level, String tag, String msg, Throwable t) {
        long ts = System.currentTimeMillis();
        Throwable tNonNull = t;

        // 1) 先写 logcat (ASSERT 级，确保不受 debuggable / 厂商日志节流影响)
        String cm = msg;
        if (tNonNull != null) cm = msg + "\n" + stackTrace(tNonNull);
        try {
            // ASSERT = Log.wtf 级别：不引发 crash，仅用来 "绕开厂商 debug 关闸"。
            Log.println(Log.ASSERT, tag, cm);
        } catch (Throwable ignore) {
            try { Log.e(tag, cm); } catch (Throwable ignored) {}
        }

        // 2) 格式化一行
        StringBuilder sb = new StringBuilder(msg.length() + 64);
        sb.append(SDF.format(new Date(ts))).append(' ');
        switch (level) {
            case V: sb.append('V'); break;
            case D: sb.append('D'); break;
            case I: sb.append('I'); break;
            case W: sb.append('W'); break;
            case E: sb.append('E'); break;
        }
        sb.append('/').append(tag).append(": ").append(msg);
        if (tNonNull != null) sb.append('\n').append(stackTrace(tNonNull));
        String line = sb.toString();

        // 3) 入环形内存 + 写文件 + 通知订阅
        Listener notifyListener = null;
        String notifySnapshot = null;
        synchronized (LOCK) {
            while (RING.size() >= MAX_LINES) RING.pollFirst();
            RING.offer(line);
            try {
                if (logStream != null) {
                    // 文件里补日期前缀，方便事后排查
                    String forFile = FILE_SDF.format(new Date(ts)) + " " +
                            levelLetter(level) + "/" + tag + ": " + msg;
                    writeToFileLocked(forFile);
                    if (tNonNull != null) writeToFileLocked(stackTrace(tNonNull));
                    writeToFileLocked("");
                    logStream.flush();
                }
            } catch (Throwable ignore) { /* 写文件失败不掉链子 */ }
            if (listener != null) {
                notifyListener = listener;
                notifySnapshot = line;
            }
        }
        if (notifyListener != null) {
            final Listener l = notifyListener;
            final String s = notifySnapshot;
            MAIN.post(() -> l.onAppended(s));
        }
    }

    private static char levelLetter(Level l) {
        switch (l) {
            case V: return 'V';
            case D: return 'D';
            case I: return 'I';
            case W: return 'W';
            case E: return 'E';
        }
        return '?';
    }

    private static void rawRingAppend(String s) {
        synchronized (LOCK) {
            while (RING.size() >= MAX_LINES) RING.pollFirst();
            RING.offer(s);
        }
    }

    /** 必须持有 LOCK。 */
    private static void writeToFileLocked(String s) {
        BufferedOutputStream os = logStream;
        if (os == null) return;
        try {
            byte[] bytes = (s + "\n").getBytes("UTF-8");
            os.write(bytes);
        } catch (Throwable ignore) {}
    }

    public static String stackTrace(Throwable t) {
        StringWriter sw = new StringWriter(1024);
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }
}
