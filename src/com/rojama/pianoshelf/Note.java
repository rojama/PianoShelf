package com.rojama.pianoshelf;

import android.graphics.PointF;

import com.xenoage.zong.core.music.Pitch;
import com.xenoage.zong.musicxml.types.MxlNote;

public class Note {
	public int measureNum;
	public int pageNum;
	public String partID;
	public MxlNote mxlNote;
	public int duration;  //64 Ϊ4��1����
	public PointF point; //��������
	public Pitch pitch; //����
	public int volume = 1;   //����
	
	public String toString(){
		return " measureNum = "+measureNum + " ;pageNum = "+pageNum + " ;partID = "+partID 
		+ " ;duration = "+duration+ " ;point = ("+point.x+":"+point.y+ ") ;pitch = "+pitch;
	}
}
