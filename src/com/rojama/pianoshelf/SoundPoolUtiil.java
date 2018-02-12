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
		//������Ƶ���ڶ�������Ϊ����������;����������Ϊ����������;���ĸ�����Ϊ���ȼ������������Ϊѭ��������0��ѭ����-1ѭ��;����������Ϊ���ʣ�����    ���0.5���Ϊ2��1���������ٶ�  
		//System.out.println(soundPoolMap.get(resid)+" | "+soundPoolIdMap.get(resid));
		soundPool.play(soundPoolIdMap.get(resid), 1, 1, Thread.NORM_PRIORITY, 0, 1);  
	}
}
