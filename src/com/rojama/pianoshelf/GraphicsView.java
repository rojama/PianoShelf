package com.rojama.pianoshelf;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.AsyncTask;
import android.os.Looper;
import android.view.View;
import android.widget.AbsoluteLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

import com.rojama.pianoshelf.musicxml.FileReader;
import com.xenoage.util.filter.AllFilter;
import com.xenoage.util.io.IO;
import com.xenoage.zong.core.music.Pitch;
import com.xenoage.zong.musicxml.types.MxlScorePartwise;
import com.xenoage.zong.symbols.SymbolPool;

public class GraphicsView {
	final int parttime = 5; // 每256TH的时间（毫秒）

	public String filepath;
	public int screenWidth;
	public int screenHeight;
	public Bitmap bitmap;
	public Context context;
	private ViewScroll detail = null;
	private LinearLayout ll;
	public CommonTransfer ct;
	private List<MxlScorePartwise> scoreList;
	public int dispalyPageNo = 1;
	public int olddispalyPageNo = 1;
	public SharedPreferences appPrefs;
	private ProgressBar progressBar = null;
	
	public boolean playOnCompleat = false;

	private SoundPool pool = new SoundPool(10, AudioManager.STREAM_MUSIC, 5);
	private HashMap<Integer, Integer> soundPoolMap = new HashMap<Integer, Integer>();

	public GraphicsView(GraphicsActivity context) {
		this.context = context;
		ll = (LinearLayout) ((Activity) context).findViewById(R.id.linearLayout_image);
		appPrefs = context.getSharedPreferences("appPreferences", Context.MODE_PRIVATE);
		progressBar = (ProgressBar) context.findViewById(R.id.progressBar);
	}

	public void showView() {
		// Toast.makeText(context, "wait display...", Toast.LENGTH_LONG).show();
		progressBar.setVisibility(View.VISIBLE);
		new AsyncLoader().execute(0);
	}

	public void reShowView() {
		// Toast.makeText(context, "wait change page...",
		// Toast.LENGTH_SHORT).show();
		new AsyncLoader().execute(1);
	}

	public void changeOrientation() {
		ct.setScreen(screenWidth, screenHeight);
		if (detail != null) {
			ll.removeView(detail);			
			detail = new ViewScroll(context, bitmap, null);
			ll.addView(detail);
		}
	}

	// AsyncTask
	class AsyncLoader extends AsyncTask<Integer, Void, Bitmap> {
		@Override
		protected Bitmap doInBackground(Integer... params) {
			try {
				if (params[0] == 0) {
					ct = new CommonTransfer();
					// Drawing commands go here
					IO.initApplication("GraphicsView");
					scoreList = FileReader.loadScores(filepath, new AllFilter());
					ct.context = context;
					ct.symbolPool = SymbolPool.loadDefault(ct.context);
					ct.setScreen(screenWidth, screenHeight);
					Paint paint = new Paint();
					paint.setColor(Color.parseColor(appPrefs.getString("foreground", "black")));
					ct.appPrefs = appPrefs;
					ct.paint = paint;
				} else if (params[0] == 1) {
					// ct.initNow();
				}
				ct.setDisPageNo(dispalyPageNo);
				for (MxlScorePartwise sub : scoreList) {
					sub.paint(ct);
				}
			} catch (Exception e) {
				e.printStackTrace();
				dispalyPageNo = olddispalyPageNo;
				return null;
			}
			return ct.bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap result/* 参数3 */) {
			super.onPostExecute(result);
			progressBar.setVisibility(View.INVISIBLE);
			if (result != null) {
				String pageInfo = String.format(context.getString(R.string.info_page_no),
						dispalyPageNo);
				Toast.makeText(context, pageInfo, Toast.LENGTH_SHORT).show();
				bitmap = result;
				if (detail == null) {
					detail = new ViewScroll(context, bitmap, null);
					ll.addView(detail);
				} else {
					detail.tv.setImageBitmap(bitmap);
				}
				if (playOnCompleat){
					play();
					playOnCompleat = false;
				}
			} else {
				Toast.makeText(context, context.getString(R.string.info_open_err),
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	public Integer time;
//	public Integer minTime;
	public Integer maxTime;

	public void play() {
		//从第一页开始
//		if (dispalyPageNo != 1){
//			dispalyPageNo = 1;
//			reShowView();
//		}
		time = 0;
//		minTime = 0;
		maxTime = 333;
		PlayThread playThread = new PlayThread();
		playThread.start();
		TimeThread timeThread = new TimeThread();
		timeThread.start();
	}

	public class TimeThread extends Thread {
		public void run() {
			while (time < maxTime) {
				try {
					this.sleep(parttime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
//				if (time < minTime) {
//					time = minTime;
//				}else{
					time++;
//				}				
//				System.out.println("time " + time);
			}
			
			//自动翻页
			if (ct.maxPage > dispalyPageNo){
				dispalyPageNo ++;
				playOnCompleat = true;
				reShowView();				
			}
		}
	}


	public class PlayThread extends Thread {
		public void run() {
			try {
				for (String partID : ct.scorePartsNotes.keySet()) {
					Vector<Note> noteflow = ct.scorePartsNotes.get(partID);
//					System.out.println("noteflow" + noteflow);

					PlayPartThread partThread = new PlayPartThread();
					partThread.noteflow = noteflow;
					partThread.start();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public class PlayPartThread extends Thread {
		public Vector<Note> noteflow;

		public void run() {
			try {
				for (Note note : noteflow) {
//					if (note.pageNum != ct.disPageNo) {
//						continue;
//					}

					// 播放音符
					PlayNoteThread noteThread = new PlayNoteThread();
					if (note.duration > maxTime) maxTime = note.duration;
//					if (note.duration < minTime) minTime = note.duration;
					noteThread.note = note;
					noteThread.start();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	
	public class PlayNoteThread extends Thread {
		public Note note;

		public void run() {
			try {
				while (note.duration > time) {
					Thread.sleep((note.duration-time)*parttime);	
				}
				
				if (note.pitch != null){
					System.out.println("play " + note.toString());
					SoundPoolUtiil.playSound(getRawID(note.pitch));
				}								
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private int getStepSemitones(int step) {
		switch (step) {
		case 0: // C
			return 0;

		case 1: // D
			return 2;

		case 2: // E
			return 4;

		case 3: // F
			return 5;

		case 4: // G
			return 7;

		case 5: // A
			return 9;

		case 6: // B
			return 11;
		}
		return 0;
	}

	public int getRawID(Pitch pitch) {
		int octave = pitch.getOctave();
		int step = pitch.getStep();
		int alter = pitch.getAlter();
		int add = getStepSemitones(step) + alter;
		if (add < 0) {
			add += 12;
			octave -= 1;
		}
		if (add >= 12) {
			add -= 12;
			octave += 1;
		}

		if (add < 1) { // C
			switch (octave) {
			case 1:
				return R.raw.c_1;
			case 2:
				return R.raw.c_2;
			case 3:
				return R.raw.c_3;
			case 4:
				return R.raw.c_4;
			case 5:
				return R.raw.c_5;
			case 6:
				return R.raw.c_6;
			}
		} else if (add < 2) { // C#
			switch (octave) {
			case 1:
				return R.raw.cs_1;
			case 2:
				return R.raw.cs_2;
			case 3:
				return R.raw.cs_3;
			case 4:
				return R.raw.cs_4;
			case 5:
				return R.raw.cs_5;
			case 6:
				return R.raw.cs_6;
			}
		} else if (add < 3) { // D
			switch (octave) {
			case 1:
				return R.raw.d_1;
			case 2:
				return R.raw.d_2;
			case 3:
				return R.raw.d_3;
			case 4:
				return R.raw.d_4;
			case 5:
				return R.raw.d_5;
			case 6:
				return R.raw.d_6;
			}
		} else if (add < 4) { // D#
			switch (octave) {
			case 1:
				return R.raw.ds_1;
			case 2:
				return R.raw.ds_2;
			case 3:
				return R.raw.ds_3;
			case 4:
				return R.raw.ds_4;
			case 5:
				return R.raw.ds_5;
			case 6:
				return R.raw.ds_6;
			}
		} else if (add < 5) { // E
			switch (octave) {
			case 1:
				return R.raw.e_1;
			case 2:
				return R.raw.e_2;
			case 3:
				return R.raw.e_3;
			case 4:
				return R.raw.e_4;
			case 5:
				return R.raw.e_5;
			case 6:
				return R.raw.e_6;
			}
		} else if (add < 6) { // F
			switch (octave) {
			case 1:
				return R.raw.f_1;
			case 2:
				return R.raw.f_2;
			case 3:
				return R.raw.f_3;
			case 4:
				return R.raw.f_4;
			case 5:
				return R.raw.f_5;
			case 6:
				return R.raw.f_6;
			}
		} else if (add < 7) { // F#
			switch (octave) {
			case 1:
				return R.raw.fs_1;
			case 2:
				return R.raw.fs_2;
			case 3:
				return R.raw.fs_3;
			case 4:
				return R.raw.fs_4;
			case 5:
				return R.raw.fs_5;
			case 6:
				return R.raw.fs_6;
			}
		} else if (add < 8) { // G
			switch (octave) {
			case 0:
				return R.raw.g_0;
			case 1:
				return R.raw.g_1;
			case 2:
				return R.raw.g_2;
			case 3:
				return R.raw.g_3;
			case 4:
				return R.raw.g_4;
			case 5:
				return R.raw.g_5;
			case 6:
				return R.raw.g_6;
			}
		} else if (add < 9) { // G#
			switch (octave) {
			case 0:
				return R.raw.gs_0;
			case 1:
				return R.raw.gs_1;
			case 2:
				return R.raw.gs_2;
			case 3:
				return R.raw.gs_3;
			case 4:
				return R.raw.gs_4;
			case 5:
				return R.raw.gs_5;
			case 6:
				return R.raw.gs_6;
			}
		} else if (add < 10) { // A
			switch (octave) {
			case 0:
				return R.raw.a_0;
			case 1:
				return R.raw.a_1;
			case 2:
				return R.raw.a_2;
			case 3:
				return R.raw.a_3;
			case 4:
				return R.raw.a_4;
			case 5:
				return R.raw.a_5;
			case 6:
				return R.raw.a_6;
			}
		} else if (add < 11) { // A#
			switch (octave) {
			case 0:
				return R.raw.as_0;
			case 1:
				return R.raw.as_1;
			case 2:
				return R.raw.as_2;
			case 3:
				return R.raw.as_3;
			case 4:
				return R.raw.as_4;
			case 5:
				return R.raw.as_5;
			case 6:
				return R.raw.as_6;
			}
		} else if (add < 12) { // B
			switch (octave) {
			case 0:
				return R.raw.b_0;
			case 1:
				return R.raw.b_1;
			case 2:
				return R.raw.b_2;
			case 3:
				return R.raw.b_3;
			case 4:
				return R.raw.b_4;
			case 5:
				return R.raw.b_5;
			case 6:
				return R.raw.b_6;
			}
		}
		return 0;
	}

}
