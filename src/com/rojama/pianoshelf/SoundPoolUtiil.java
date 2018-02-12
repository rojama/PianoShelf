package com.rojama.pianoshelf;

import java.util.HashMap;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

public class SoundPoolUtiil {
	final private static SoundPool soundPool = new SoundPool(20, AudioManager.STREAM_MUSIC, 0);
	//resid / soundid
	final private static HashMap<Integer, Integer> soundPoolIdMap = new HashMap<Integer, Integer>();

	public static void loadSound (Context context){
		for (int resid=R.raw.a_0; resid<=R.raw.gs_6; resid++){
			final int sourceid = soundPool.load(context, resid, 0);
			soundPoolIdMap.put(resid, sourceid);
		}
	}
	
	public static void playSound (int resid){
		//播放音频，第二个参数为左声道音量;第三个参数为右声道音量;第四个参数为优先级；第五个参数为循环次数，0不循环，-1循环;第六个参数为速率，速率    最低0.5最高为2，1代表正常速度  
		//System.out.println(soundPoolMap.get(resid)+" | "+soundPoolIdMap.get(resid));
		soundPool.play(soundPoolIdMap.get(resid), 1, 1, Thread.NORM_PRIORITY, 0, 1);  
	}
}
