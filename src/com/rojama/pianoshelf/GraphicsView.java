package com.rojama.pianoshelf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.rojama.pianoshelf.musicxml.FileReader;
import com.xenoage.util.filter.AllFilter;
import com.xenoage.util.io.IO;
import com.xenoage.zong.core.music.Pitch;
import com.xenoage.zong.musicxml.types.MxlScorePartwise;
import com.xenoage.zong.symbols.SymbolPool;

/**
 * 乐谱查看 + 播放核心控制器。
 *
 * 重写要点 (Bug Fixes + Optimization):
 *
 * 1) [THREAD EXPLOSION FIX] 原实现每个 Note 创建独立 PlayNoteThread，
 *    一首中等乐谱 (500 音符) 就会创建 500+ 线程（OOM/卡顿）。
 *    → 改用单一 ScheduledExecutorService + per-tick 调度。
 *
 * 2) [PERFORMANCE] getRawID() 原先有 ~12 层 switch-case 嵌套约 80 条分支。
 *    → 改为一次性构建二维查表数组 (O(1))。
 *
 * 3) [SAFETY] scoreList / ct 未初始化前翻页会 NPE → 添加 null 守卫。
 *
 * 4) [LIFECYCLE] 播放线程、executor、SoundPool 在 Activity 销毁时必须释放。
 *    → 增加 shutdown() 方法并提供 isPlaying 标志。
 *
 * 5) [DEPRECATED API] AsyncTask 在 API 30+ 废弃 → 改为 Handler + Executor。
 */
public class GraphicsView {
	private static final String TAG = "GraphicsView";

	// 每 256TH note = parttime ms。越小节奏越快。
	// (保持与原始值一致以避免节奏变化)
	static final int PARTTIME_MS = 5;

	public String filepath;
	public int screenWidth;
	public int screenHeight;
	public volatile Bitmap bitmap;
	public Context context;
	private ViewScroll detail = null;
	private LinearLayout ll;
	public CommonTransfer ct;
	private List<MxlScorePartwise> scoreList;
	public int dispalyPageNo = 1;
	public int olddispalyPageNo = 1;
	public SharedPreferences appPrefs;
	private ProgressBar progressBar = null;
	private android.widget.TextView progressText = null;
	private android.widget.ScrollView logScroll = null;
	private android.widget.TextView logText = null;
	public boolean playOnCompleat = false;

	// -------- playback scheduler (replaces Thread-per-note) --------
	private final Handler mainHandler = new Handler(Looper.getMainLooper());
	private ScheduledExecutorService scheduler;
	private ScheduledFuture<?> tickFuture;
	private ScheduledFuture<?> autoPageFuture;
	private final AtomicBoolean playing = new AtomicBoolean(false);
	private final AtomicInteger timeCounter = new AtomicInteger(0);
	private int maxTime = 0;

	// -------- pitch → R.raw id lookup table (O(1)) --------
	private static final int ROWS = 12;  // add = 0..11
	private static final int COLS = 8;   // octave = 0..7 (we use 0..6, buffer extra)
	private static final int[][] RAW_ID_TABLE = new int[ROWS][COLS];
	private static volatile boolean lookupBuilt = false;

	/** 加载阶段百分比权重（总和 100）。 */
	private static final int ST_CT = 5;      // 01. CommonTransfer 初始化
	private static final int ST_IO = 10;     // 02. IO.initApplication
	private static final int ST_LOAD = 50;   // 03. FileReader.loadScores (大活)
	private static final int ST_SYM = 62;    // 04. 加载 SymbolPool
	private static final int ST_SCR = 70;    // 05. setScreen（创建 Bitmap）
	private static final int ST_PAINT = 98;  // 06. paint 渲染
	private static final int ST_DONE = 100;  // 07. 完成

	private volatile Throwable lastLoadError;

	public GraphicsView(GraphicsActivity context) {
		this.context = context;
		ll = (LinearLayout) context.findViewById(R.id.linearLayout_image);
		appPrefs = context.getSharedPreferences("appPreferences", Context.MODE_PRIVATE);
		progressBar = (ProgressBar) context.findViewById(R.id.progressBar);
		progressText = (android.widget.TextView) context.findViewById(R.id.progressText);
		logScroll = (android.widget.ScrollView) context.findViewById(R.id.logScroll);
		logText = (android.widget.TextView) context.findViewById(R.id.logText);
		ensureLookupBuilt();
		wireDebugLogPanel();
	}

	private void wireDebugLogPanel() {
		if (logText == null) return;
		// 启动时把 ring buffer 里现存的历史也先刷出来
		logText.setText(DebugLog.dumpAll());
		autoScrollLog();
		DebugLog.setListener(new DebugLog.Listener() {
			@Override public void onAppended(final String deltaOrAll) {
				if (logText == null) return;
				// setListener 初次回调可能是一大段（整份历史），之后每次都是新增行。
				CharSequence old = logText.getText();
				if (old == null || old.length() == 0) {
					logText.setText(deltaOrAll);
				} else {
					if (deltaOrAll != null && deltaOrAll.length() > 0) {
						logText.append(deltaOrAll);
						if (!deltaOrAll.endsWith("\n")) logText.append("\n");
					}
				}
				autoScrollLog();
			}
		});
	}

	private void autoScrollLog() {
		if (logScroll == null || logText == null) return;
		logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
	}

	/** 设置顶部横向进度条 + 文字百分比描述。可后台线程调用。 */
	private void postProgress(final int pct, final String stageText) {
		mainHandler.post(() -> {
			if (progressBar != null) {
				progressBar.setProgress(Math.max(0, Math.min(100, pct)));
				progressBar.setVisibility(View.VISIBLE);
			}
			if (progressText != null) {
				progressText.setText(String.format(java.util.Locale.US,
						"%s  %d%%", stageText == null ? "" : stageText, pct));
			}
		});
	}

	public void showView() {
		progressBar.setVisibility(View.VISIBLE);
		lastLoadError = null;
		DebugLog.i(TAG, "=======================================");
		DebugLog.i(TAG, "开始加载乐谱: " + filepath);
		postProgress(0, "启动加载");
		executeAsync(new Runnable() {
			// doLoad 声明 throws checked Exception；Runnable.run() 不能抛 checked，
			// 这里包装为 RuntimeException。外层 executeAsync 会 catch(Throwable) 统一兜底。
			@Override public void run() {
				try { doLoad(true); }
				catch (Exception e) { throw new RuntimeException(e); }
			}
		});
	}

	public void reShowView() {
		progressBar.setVisibility(View.VISIBLE);
		DebugLog.d(TAG, "重新渲染第 " + dispalyPageNo + " 页（不重新解析）");
		executeAsync(new Runnable() {
			@Override public void run() {
				try { doLoad(false); }
				catch (Exception e) { throw new RuntimeException(e); }
			}
		});
	}

	public void changeOrientation() {
		if (ct != null) {
			ct.setScreen(screenWidth, screenHeight);
		}
		if (detail != null && ll != null) {
			ll.removeView(detail);
			detail = new ViewScroll(context, bitmap, null);
			ll.addView(detail);
		}
	}

	/**
	 * Stop playback + release thread-pool. Should be called when host Activity
	 * is destroyed.
	 */
	public void shutdown() {
		stopPlayback();
		if (scheduler != null) {
			try { scheduler.shutdownNow(); } catch (Throwable ignored) {}
			scheduler = null;
		}
		DebugLog.setListener(null);
	}

	private void executeAsync(final Runnable work) {
		new Thread(new Runnable() {
			@Override public void run() {
				Bitmap bmp = null;
				long t0 = System.currentTimeMillis();
				try {
					work.run();
					bmp = (ct == null) ? null : ct.bitmap;
					long t1 = System.currentTimeMillis();
					DebugLog.i(TAG, String.format("渲染完成 耗时=%d ms  页数=%d  bitmap=%s",
							(t1 - t0), dispalyPageNo,
							bmp == null ? "null" : (bmp.getWidth() + "x" + bmp.getHeight())));
				} catch (Throwable t) {
					DebugLog.e(TAG, "加载/渲染异常（详情见下方堆栈）", t);
					lastLoadError = t;
					dispalyPageNo = olddispalyPageNo;
					bmp = null;
				}
				final Bitmap fbmp = bmp;
				mainHandler.post(new Runnable() {
					@Override public void run() { onLoadFinished(fbmp); }
				});
			}
		}, "PianoShelf-Loader").start();
	}

	/** Async body that builds the CommonTransfer + paints the target page. */
	private void doLoad(boolean firstTime) throws Exception {
		if (firstTime) {
			postProgress(ST_CT, "初始化渲染上下文");
			ct = new CommonTransfer();
			postProgress(ST_IO, "初始化 IO 子系统");
			DebugLog.d(TAG, "IO.initApplication");
			IO.initApplication("GraphicsView");
			postProgress(ST_LOAD - 5, "解析 MusicXML/MXL 0%");
			DebugLog.d(TAG, "FileReader.loadScores 开始（最长耗时步骤）");
			scoreList = FileReader.loadScores(filepath, new AllFilter());
			DebugLog.i(TAG, "FileReader.loadScores 完成，scoreList.size=" + (scoreList == null ? 0 : scoreList.size()));
			postProgress(ST_LOAD, "解析完成");
			postProgress(ST_SYM, "加载 SymbolPool (字体符号)");
			ct.context = context;
			ct.symbolPool = SymbolPool.loadDefault(ct.context);
			if (ct.symbolPool == null) {
				throw new RuntimeException("SymbolPool.loadDefault 返回 null，无法加载乐谱符号资源，请检查 logcat 中 SymbolPool 的错误日志");
			}
			DebugLog.d(TAG, "SymbolPool.loadDefault 完成，symbols=" + (ct.symbolPool.getSymbolCount()));
			postProgress(ST_SCR, "创建画布 bitmap " + screenWidth + "x" + screenHeight);
			ct.setScreen(screenWidth, screenHeight);
			Paint paint = new Paint();
			try {
				paint.setColor(Color.parseColor(appPrefs.getString("foreground", "black")));
			} catch (Throwable ignore) {
				paint.setColor(Color.BLACK);
			}
			ct.appPrefs = appPrefs;
			ct.paint = paint;
		}
		if (ct == null || scoreList == null) {
			throw new IllegalStateException("Cannot render: init failed (ct=" + ct + ", scoreList=" + scoreList + ")");
		}
		postProgress(ST_PAINT - 15, "渲染第 " + dispalyPageNo + " 页 0%");
		DebugLog.d(TAG, "ct.setDisPageNo(dispalyPageNo=" + dispalyPageNo + ")");
		ct.setDisPageNo(dispalyPageNo);
		int total = scoreList.size();
		int done = 0;
		for (MxlScorePartwise sub : scoreList) {
			int pct = ST_SCR + (ST_PAINT - ST_SCR) * done / Math.max(1, total);
			postProgress(pct, "渲染 " + (done + 1) + "/" + total);
			sub.paint(ct);
			done++;
		}
		postProgress(ST_DONE, "渲染完成");
	}

	private void onLoadFinished(Bitmap result) {
		if (progressBar != null) {
			if (result != null) {
				progressBar.setProgress(100);
			}
			progressBar.setVisibility(View.INVISIBLE);
		}
		if (progressText != null) {
			if (result != null) {
				progressText.setText("完成 100%  (点击菜单/播放键听声音)");
			} else if (lastLoadError != null) {
				progressText.setText("❌ 加载失败：见日志面板堆栈");
				progressText.setTextColor(0xFFD50000);
			} else {
				progressText.setText("未渲染出结果");
			}
		}
		if (result != null) {
			String pageInfo = String.format(
					context.getString(R.string.info_page_no), dispalyPageNo);
			Toast.makeText(context, pageInfo, Toast.LENGTH_SHORT).show();
			bitmap = result;
			if (detail == null) {
				detail = new ViewScroll(context, bitmap, null);
				if (ll != null) ll.addView(detail);
			} else {
				detail.tv.setImageBitmap(bitmap);
			}
			if (playOnCompleat) {
				playOnCompleat = false;
				play();
			}
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append(context.getString(R.string.info_open_err));
			if (lastLoadError != null) {
				sb.append('\n').append(lastLoadError.getClass().getSimpleName())
				  .append(": ").append(lastLoadError.getLocalizedMessage());
				Throwable cause = lastLoadError.getCause();
				while (cause != null) {
					sb.append("\n  caused by: ").append(cause.getClass().getSimpleName())
					  .append(": ").append(cause.getLocalizedMessage());
					cause = cause.getCause();
				}
			}
			Toast.makeText(context, sb.toString(), Toast.LENGTH_LONG).show();
			// 出错时把日志面板置顶露出
			View panel = ((android.app.Activity) context).findViewById(R.id.debug_log_panel);
			if (panel != null && panel.getVisibility() != View.VISIBLE) {
				panel.setVisibility(View.VISIBLE);
			}
			autoScrollLog();
		}
	}

	// =======================================================
	// Playback (fixed scheduler, replaces thread-per-note)
	// =======================================================

	public synchronized void play() {
		if (ct == null || ct.scorePartsNotes == null) {
			Toast.makeText(context,
					context.getString(R.string.info_open_err),
					Toast.LENGTH_SHORT).show();
			return;
		}
		stopPlayback();

		// Compute playback note plan: Map<tick, List<Note-to-play-at-tick>>
		final Map<Integer, Vector<Note>> plan = buildPlaybackPlan();
		if (plan.isEmpty()) return;

		timeCounter.set(0);
		maxTime = computeMaxTime(plan);
		playing.set(true);

		// Start scheduler with 2 threads (tick + spare)
		scheduler = Executors.newScheduledThreadPool(2);

		// Tick: every PARTTIME_MS advance clock; dispatch notes scheduled at the new tick value
		tickFuture = scheduler.scheduleAtFixedRate(new Runnable() {
			@Override public void run() {
				int now = timeCounter.incrementAndGet();
				Vector<Note> notes = plan.get(now);
				if (notes != null) {
					for (Note n : notes) {
						if (n.pitch != null) {
							int raw = getRawID(n.pitch);
							if (raw > 0) SoundPoolUtiil.playSound(raw);
						}
					}
				}
				if (now >= maxTime) {
					stopPlayback();
					// Schedule auto page-flip on main thread (safely)
					mainHandler.post(new Runnable() {
						@Override public void run() { autoAdvancePage(); }
					});
				}
			}
		}, PARTTIME_MS, PARTTIME_MS, TimeUnit.MILLISECONDS);
	}

	/** Return true if playback is currently running. */
	public boolean isPlaying() {
		return playing.get();
	}

	/** Stop playback and release its scheduled tasks (but keep pool for reuse). */
	public synchronized void stopPlayback() {
		playing.set(false);
		if (tickFuture != null) {
			try { tickFuture.cancel(false); } catch (Throwable ignored) {}
			tickFuture = null;
		}
		if (autoPageFuture != null) {
			try { autoPageFuture.cancel(false); } catch (Throwable ignored) {}
			autoPageFuture = null;
		}
	}

	private void autoAdvancePage() {
		if (ct != null && ct.maxPage > dispalyPageNo) {
			dispalyPageNo++;
			playOnCompleat = true;
			reShowView();
		}
	}

	/** Build per-tick note list from CommonTransfer note accumulator. */
	private Map<Integer, Vector<Note>> buildPlaybackPlan() {
		Map<Integer, Vector<Note>> plan = new HashMap<Integer, Vector<Note>>();
		for (Vector<Note> notes : ct.scorePartsNotes.values()) {
			if (notes == null) continue;
			for (Note n : notes) {
				int tick = n.duration;
				Vector<Note> bucket = plan.get(tick);
				if (bucket == null) {
					bucket = new Vector<Note>(4);
					plan.put(tick, bucket);
				}
				bucket.add(n);
			}
		}
		return plan;
	}

	private static int computeMaxTime(Map<Integer, Vector<Note>> plan) {
		int max = 0;
		for (Integer t : plan.keySet()) {
			if (t > max) max = t;
		}
		// Add small tail so final notes ring out a bit (page flip scheduling safety)
		return max + 32;
	}

	// =======================================================
	// Pitch → raw id lookup (O(1) table)
	// =======================================================

	private static synchronized void ensureLookupBuilt() {
		if (lookupBuilt) return;
		// Initialize to 0 (invalid)
		for (int r = 0; r < ROWS; r++) {
			for (int c = 0; c < COLS; c++) RAW_ID_TABLE[r][c] = 0;
		}
		// Step → add (within octave), offset from C = 0
		// 0:C, 1:C#, 2:D, 3:D#, 4:E, 5:F, 6:F#, 7:G, 8:G#, 9:A, 10:A#, 11:B
		fillRow(0,  new int[] { R.raw.c_1, R.raw.c_2, R.raw.c_3, R.raw.c_4, R.raw.c_5, R.raw.c_6 }, 1);
		fillRow(1,  new int[] { R.raw.cs_1, R.raw.cs_2, R.raw.cs_3, R.raw.cs_4, R.raw.cs_5, R.raw.cs_6 }, 1);
		fillRow(2,  new int[] { R.raw.d_1, R.raw.d_2, R.raw.d_3, R.raw.d_4, R.raw.d_5, R.raw.d_6 }, 1);
		fillRow(3,  new int[] { R.raw.ds_1, R.raw.ds_2, R.raw.ds_3, R.raw.ds_4, R.raw.ds_5, R.raw.ds_6 }, 1);
		fillRow(4,  new int[] { R.raw.e_1, R.raw.e_2, R.raw.e_3, R.raw.e_4, R.raw.e_5, R.raw.e_6 }, 1);
		fillRow(5,  new int[] { R.raw.f_1, R.raw.f_2, R.raw.f_3, R.raw.f_4, R.raw.f_5, R.raw.f_6 }, 1);
		fillRow(6,  new int[] { R.raw.fs_1, R.raw.fs_2, R.raw.fs_3, R.raw.fs_4, R.raw.fs_5, R.raw.fs_6 }, 1);
		fillRow(7,  new int[] { R.raw.g_0, R.raw.g_1, R.raw.g_2, R.raw.g_3, R.raw.g_4, R.raw.g_5, R.raw.g_6 }, 0);
		fillRow(8,  new int[] { R.raw.gs_0, R.raw.gs_1, R.raw.gs_2, R.raw.gs_3, R.raw.gs_4, R.raw.gs_5, R.raw.gs_6 }, 0);
		fillRow(9,  new int[] { R.raw.a_0, R.raw.a_1, R.raw.a_2, R.raw.a_3, R.raw.a_4, R.raw.a_5, R.raw.a_6 }, 0);
		fillRow(10, new int[] { R.raw.as_0, R.raw.as_1, R.raw.as_2, R.raw.as_3, R.raw.as_4, R.raw.as_5, R.raw.as_6 }, 0);
		fillRow(11, new int[] { R.raw.b_0, R.raw.b_1, R.raw.b_2, R.raw.b_3, R.raw.b_4, R.raw.b_5, R.raw.b_6 }, 0);
		lookupBuilt = true;
	}

	private static void fillRow(int add, int[] ids, int octaveOffset) {
		for (int i = 0; i < ids.length; i++) {
			int oct = octaveOffset + i;
			if (oct >= 0 && oct < COLS) RAW_ID_TABLE[add][oct] = ids[i];
		}
	}

	private static int getStepSemitones(int step) {
		// C D E F G A B → 0 2 4 5 7 9 11
		switch (step) {
			case 0: return 0;
			case 1: return 2;
			case 2: return 4;
			case 3: return 5;
			case 4: return 7;
			case 5: return 9;
			case 6: return 11;
		}
		return 0;
	}

	public int getRawID(Pitch pitch) {
		if (pitch == null) return 0;
		int octave = pitch.getOctave();
		int step = pitch.getStep();
		int alter = pitch.getAlter();
		int add = getStepSemitones(step) + alter;
		// normalize to [0, 12)
		while (add < 0) { add += 12; octave -= 1; }
		while (add >= 12) { add -= 12; octave += 1; }
		if (add < 0 || add >= ROWS || octave < 0 || octave >= COLS) return 0;
		return RAW_ID_TABLE[add][octave];
	}
}
