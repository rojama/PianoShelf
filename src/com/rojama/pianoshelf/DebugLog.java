package com.rojama.pianoshelf;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * 调试日志：同时输出到 logcat (DEBUG) 并在内存里留最近 N 行（环形），
 * 供 GraphicsActivity 的日志面板实时滚动显示。
 *
 * 典型用法：
 *   DebugLog.d("TAG", "打开文件：" + path);
 *   DebugLog.e("TAG", "解析失败", e);   // 自动追加异常堆栈
 *   // 外部订阅：
 *   DebugLog.setListener(lines -> ui.post(...));
 */
public final class DebugLog {
    public static final int MAX_LINES = 200;

    public enum Level { V, D, I, W, E }

    public interface Listener {
        void onAppended(String line);
    }

    private static final Object LOCK = new Object();
    private static final ArrayDeque<String> RING = new ArrayDeque<>(MAX_LINES + 8);
    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static Listener listener;
    private static String lastFlushSnapshot;

    private DebugLog() {}

    /** 订阅：会立刻把当前所有已有行一次性回调（在主线程），之后每行也回调。null 取消订阅。 */
    public static void setListener(Listener l) {
        synchronized (LOCK) {
            listener = l;
            if (l != null) {
                final List<String> snap = new ArrayList<>(RING);
                final Listener copy = l;
                MAIN.post(() -> {
                    StringBuilder sb = new StringBuilder(2048);
                    for (String s : snap) sb.append(s).append('\n');
                    copy.onAppended(sb.toString());
                });
            }
        }
    }

    /** 一次性取所有行（启动/重进 Activity 时可用）。 */
    public static String dumpAll() {
        synchronized (LOCK) {
            StringBuilder sb = new StringBuilder(2048);
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

    /**
     * 同时：
     *   1) 输出到 logcat（不污染用户 UI，只给开发用）
     *   2) 追加到环形内存 buffer
     *   3) 如果有 UI 订阅器 → MAIN.post 回调新增行
     */
    private static void log(Level level, String tag, String msg, Throwable t) {
        long ts = System.currentTimeMillis();
        // 1) logcat 输出
        try {
            String cm = t == null ? msg : msg + "\n" + stackTrace(t);
            switch (level) {
                case V: Log.v(tag, cm); break;
                case D: Log.d(tag, cm); break;
                case I: Log.i(tag, cm); break;
                case W: Log.w(tag, cm); break;
                case E: Log.e(tag, cm); break;
            }
        } catch (Throwable ignore) {}

        // 2) 格式化行（紧凑版：用于 UI）
        StringBuilder sb = new StringBuilder();
        sb.append(SDF.format(new Date(ts))).append(' ');
        switch (level) {
            case V: sb.append('V'); break;
            case D: sb.append('D'); break;
            case I: sb.append('I'); break;
            case W: sb.append('W'); break;
            case E: sb.append('E'); break;
        }
        sb.append('/').append(tag).append(": ").append(msg);
        if (t != null) sb.append('\n').append(stackTrace(t));
        String line = sb.toString();

        Listener notifyListener = null;
        String notifySnapshot = null;
        synchronized (LOCK) {
            // 只保留 MAX_LINES，超了就从队头弹
            while (RING.size() >= MAX_LINES) RING.pollFirst();
            RING.offer(line);
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

    public static String stackTrace(Throwable t) {
        StringWriter sw = new StringWriter(512);
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }
}
