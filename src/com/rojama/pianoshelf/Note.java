package com.rojama.pianoshelf;

import android.graphics.PointF;

import com.xenoage.zong.core.music.Pitch;
import com.xenoage.zong.musicxml.types.MxlNote;

public class Note {
	public int measureNum;
	public int pageNum;
	public String partID;
	public MxlNote mxlNote;
	public int duration;  //64 为4分1音符
	public PointF point; //音符坐标
	public Pitch pitch; //音调
	public int volume = 1;   //音量
	
	public String toString(){
		return " measureNum = "+measureNum + " ;pageNum = "+pageNum + " ;partID = "+partID 
		+ " ;duration = "+duration+ " ;point = ("+point.x+":"+point.y+ ") ;pitch = "+pitch;
	}
}
