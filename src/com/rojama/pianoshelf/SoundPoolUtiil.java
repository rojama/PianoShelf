package com.rojama.pianoshelf;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Build;
import android.media.SoundPool;
import android.util.Log;

/**
 * SoundPool 管理器。
 *
 * 修复:
 *  - 旧 SoundPool(int, int, int) 构造器在 API 21+ 已废弃 → 使用 SoundPool.Builder
 *  - 未提供 release() 方法 → 新增 release() 避免内存/音频资源泄漏
 *  - 未加载完成即调用 play() 时可能空指针 → 添加加载状态校验 + 计数回调等待
 *  - R.raw.id 迭代使用反射方式，避免依赖资源 ID 在 R 类中连续分配（不同构建不保证）
 */
public class SoundPoolUtiil {
	private static final String TAG = "SoundPoolUtiil";
	private static final int MAX_STREAMS = 20;

	private static volatile SoundPool soundPool;
	// resid -> soundPool streamId
	private static final HashMap<Integer, Integer> soundPoolIdMap = new HashMap<Integer, Integer>();
	private static final AtomicBoolean loaded = new AtomicBoolean(false);
	private static int totalToLoad = 0;
	private static int totalLoaded = 0;
	private static final Object loadLock = new Object();

	private SoundPoolUtiil() { /* utility */ }

	private static SoundPool createSoundPool() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			AudioAttributes attrs = new AudioAttributes.Builder()
					.setUsage(AudioAttributes.USAGE_MEDIA)
					.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
					.setLegacyStreamType(AudioManager.STREAM_MUSIC)
					.build();
			return new SoundPool.Builder()
					.setMaxStreams(MAX_STREAMS)
					.setAudioAttributes(attrs)
					.build();
		} else {
			// Deprecated but required for API < 21
			@SuppressWarnings("deprecation")
			SoundPool sp = new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, 0);
			return sp;
		}
	}

	private static synchronized SoundPool ensurePool() {
		if (soundPool == null) {
			soundPool = createSoundPool();
			soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
				@Override
				public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
					if (status == 0) {
						synchronized (loadLock) {
							totalLoaded++;
							if (totalLoaded >= totalToLoad) {
								loaded.set(true);
								loadLock.notifyAll();
							}
						}
					}
				}
			});
		}
		return soundPool;
	}

	/**
	 * Preload all note audios from res/raw into SoundPool.
	 *
	 * Loads via reflection over R.raw.* fields whose names match piano keys,
	 * which works reliably across all build systems (Ant / Gradle).
	 */
	public static void loadSound(Context context) {
		if (context == null) return;
		SoundPool pool = ensurePool();
		soundPoolIdMap.clear();
		totalLoaded = 0;
		loaded.set(false);

		// R.raw.* field discovery by name convention
		final String[] names = buildResourceNames();
		totalToLoad = names.length;
		int loadedCount = 0;
		for (String name : names) {
			try {
				int id = R.raw.class.getField(name).getInt(null);
				int streamId = pool.load(context, id, 1);
				soundPoolIdMap.put(id, streamId);
				loadedCount++;
			} catch (Throwable t) {
				Log.w(TAG, "Skipping missing raw resource: " + name);
			}
		}
		Log.d(TAG, "Enqueued sound loads: " + loadedCount + "/" + totalToLoad);

		// If all synchronous loads failed to produce count, mark as "loaded" to avoid blocking forever
		if (loadedCount == 0) {
			synchronized (loadLock) {
				loaded.set(true);
				loadLock.notifyAll();
			}
		}
	}

	/**
	 * Play a single note audio.
	 *
	 * @param resId R.raw.* id (returned from {@link GraphicsView#getRawID})
	 */
	public static void playSound(int resId) {
		if (resId <= 0) return;
		Integer streamId = soundPoolIdMap.get(resId);
		if (streamId == null) {
			Log.w(TAG, "No sound loaded for resId=" + resId);
			return;
		}
		SoundPool pool = soundPool;
		if (pool == null) return;

		// Wait briefly if still loading (best-effort).
		if (!loaded.get()) {
			synchronized (loadLock) {
				if (!loaded.get()) {
					try { loadLock.wait(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
				}
			}
		}

		// play(streamId, leftVol, rightVol, priority, loop, rate)
		pool.play(streamId, 1.0f, 1.0f, Thread.NORM_PRIORITY, 0, 1.0f);
	}

	/**
	 * Release SoundPool resources. Call from Activity.onDestroy() / service teardown.
	 * Safe to call multiple times.
	 */
	public static synchronized void release() {
		SoundPool pool = soundPool;
		if (pool != null) {
			try { pool.release(); } catch (Exception ignored) {}
			soundPool = null;
		}
		soundPoolIdMap.clear();
		loaded.set(false);
		totalToLoad = 0;
		totalLoaded = 0;
	}

	/**
	 * Generate R.raw field names for all supported keys.
	 * Format: {step}_{octave} where step in { c, cs, d, ds, e, f, fs, g, gs, a, as, b }
	 */
	private static String[] buildResourceNames() {
		final String[] steps = new String[] {
				"a", "as", "b",
				"c", "cs", "d", "ds", "e", "f", "fs", "g", "gs"
		};
		// Octave ranges per step (as used in GraphicsView#getRawID):
		//   g, gs, a, as, b: 0..6
		//   c, cs, d, ds, e, f, fs: 1..6
		java.util.ArrayList<String> list = new java.util.ArrayList<String>();
		for (String s : steps) {
			final int startOctave = (s.equals("c") || s.equals("cs") || s.equals("d") || s.equals("ds")
					|| s.equals("e") || s.equals("f") || s.equals("fs")) ? 1 : 0;
			for (int oct = startOctave; oct <= 6; oct++) {
				list.add(s + "_" + oct);
			}
		}
		return list.toArray(new String[0]);
	}
}
