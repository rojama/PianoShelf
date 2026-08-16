package com.rojama.pianoshelf;

import android.graphics.PointF;

import com.xenoage.zong.core.music.Pitch;
import com.xenoage.zong.musicxml.types.MxlNote;

public class Note {
	public int measureNum;
	public int pageNum;
	public String partID;
	public MxlNote mxlNote;
	/**
	 * 音符开始时刻，单位 = "该 part 当时的 divisions"（也就是 MusicXML 里的 division 单位）。
	 * 统一不做 *64/divisions 换算，避免 XML 中途 divisions 变化造成不同 part 间刻度不一致。
	 * 真正的播放 tick（64 = 四分音符）换算在 GraphicsView.buildPlaybackPlan 中按 note.divisions 完成。
	 */
	public int duration;
	/** 记录写入 duration 时这个 part 使用的 divisions，用于统一换算播放 tick。 */
	public int divisions;
	public PointF point; //音符坐标
	public Pitch pitch; //音调
	public int volume = 1;   //音量
	
	public String toString(){
		return " measureNum = "+measureNum + " ;pageNum = "+pageNum + " ;partID = "+partID 
		+ " ;duration = "+duration+"(div=" + divisions + ")" + " ;point = ("+point.x+":"+point.y+ ") ;pitch = "+pitch;
	}
}
