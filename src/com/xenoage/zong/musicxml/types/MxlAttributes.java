package com.xenoage.zong.musicxml.types;

import org.w3c.dom.Element;

import android.content.Context;
import android.content.res.Resources;

import com.rojama.pianoshelf.PaintTransfer;
import com.xenoage.pdlib.PVector;
import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.core.music.clef.ClefType;
import com.xenoage.zong.musicxml.types.choice.MxlMusicDataContent;
import com.xenoage.zong.musicxml.types.choice.MxlTimeContent;
import com.xenoage.zong.musicxml.types.choice.MxlTimeContent.MxlTimeContentType;
import com.xenoage.zong.musicxml.types.enums.MxlClefSign;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import com.xenoage.zong.symbols.Symbol;
import com.xenoage.zong.symbols.common.CommonSymbol;

@IncompleteMusicXML(missing = "editorial,part-symbol,instruments,staff-details,directive,measure-style", partly = "key,time,clef", children = "key,time,clef")
public final class MxlAttributes implements MxlMusicDataContent {
	public static final String ELEM_NAME = "attributes";

	@MaybeNull
	private final Integer divisions;

	@MaybeNull
	private final MxlKey key;

	@MaybeNull
	private final MxlTime time;

	@MaybeNull
	private final Integer staves;

	@MaybeNull
	private final PVector<MxlClef> clef;

	@MaybeNull
	private final MxlTranspose transpose;

	public MxlAttributes(Integer divisions, MxlKey key, MxlTime time, Integer staves, PVector<MxlClef> clef,
			MxlTranspose transpose) {
		this.divisions = divisions;
		this.key = key;
		this.time = time;
		this.staves = staves;
		this.clef = clef;
		this.transpose = transpose;
	}

	@MaybeNull
	public Integer getDivisions() {
		return this.divisions;
	}

	@MaybeNull
	public MxlKey getKey() {
		return this.key;
	}

	@MaybeNull
	public MxlTime getTime() {
		return this.time;
	}

	@MaybeNull
	public Integer getStaves() {
		return this.staves;
	}

	@MaybeNull
	public PVector<MxlClef> getClef() {
		return this.clef;
	}

	@MaybeNull
	public MxlTranspose getTranspose() {
		return this.transpose;
	}

	@Override
	public MxlMusicDataContent.MxlMusicDataContentType getMusicDataContentType() {
		return MxlMusicDataContent.MxlMusicDataContentType.Attributes;
	}

	@NeverNull
	public static MxlAttributes read(Element e) {
		Integer divisions = null;
		MxlKey key = null;
		MxlTime time = null;
		Integer staves = null;
		PVector<MxlClef> clef =  PVector.pvec();
		MxlTranspose transpose = null;
		for (Element child : XMLReader.elements(e)) {
			String n = child.getNodeName();
			switch (n.charAt(0)) {
			case 'c':
				if (!"clef".equals(n))
					break;
				clef = clef.plus(MxlClef.read(child));
				break;
			case 'd':
				if (!"divisions".equals(n))
					break;
				divisions = Integer.valueOf(Parse.parseInt(child));
				break;
			case 'k':
				if (!"key".equals(n))
					break;
				key = MxlKey.read(child);
				break;
			case 's':
				if (!"staves".equals(n))
					break;
				staves = Integer.valueOf(Parse.parseInt(child));
				break;
			case 't':
				if ("time".equals(n)) {
					time = MxlTime.read(child);
				} else {
					if (!"transpose".equals(n))
						break;
					transpose = MxlTranspose.read(child);
				}
			}
		}
		return new MxlAttributes(divisions, key, time, staves, clef, transpose);
	}

	@Override
	public void write(Element parent) {
		Element e = XMLWriter.addElement("attributes", parent);
		XMLWriter.addElement("divisions", this.divisions, e);
		if (this.key != null)
			this.key.write(e);
		if (this.time != null)
			this.time.write(e);
		XMLWriter.addElement("staves", this.staves, e);
		for (MxlClef clef : this.clef)
			clef.write(e);
		if (this.transpose != null)
			this.transpose.write(e);
	}

	@Override
	public void print(PaintTransfer pt) {				
		//谱号
		for (MxlClef clef : this.getClef()) {
			if (clef.getSign() == MxlClefSign.G) {
				pt.nowClefType.put(clef.getNumber(), ClefType.G);				
			}else if (clef.getSign() == MxlClefSign.F) {
				pt.nowClefType.put(clef.getNumber(), ClefType.F);	
			}
		}
		
		//音调符号
		if (this.getKey() != null){
			pt.nowFifths = this.getKey().getFifths();
		}
		
		//节拍
		if (this.getTime() != null){
			pt.nowTime = this.getTime();
		}		

		//取得音长分部
		if (this.getDivisions() != null){
			pt.divisions = this.getDivisions();
		}
		
		if (pt.nowPage != pt.ct.getDisPageNo())
			return;			
		
		//是否只打印节拍
		boolean printTimeOnly = false;
		if (this.getTime() != null && this.getClef().isEmpty() && !pt.isNewSystem){
			printTimeOnly = true;
		}
		
		pt.printHand(printTimeOnly);
		
		
//		//节拍
//		if (this.getTime() != null){
//			pt.nowTime = this.getTime();
//			if (pt.oldX == 0)
//				pt.oldX += pt.ct.SPACE;
//			
//			if (this.getTime().getContent().getTimeContentType() == MxlTimeContentType.NormalTime){
//				MxlNormalTime timecon = (MxlNormalTime) this.getTime().getContent();
//				Symbol symbolUp = pt.ct.symbolPool.getSymbol(CommonSymbol.getDigit(timecon.getBeats()));
//				Symbol symbolDown = pt.ct.symbolPool.getSymbol(CommonSymbol.getDigit(timecon.getBeatType()));
//				pt.drawBitmap(symbolUp.getBitmap(), pt.measureLeft+pt.oldX, pt.measureUp);
//				pt.drawBitmap(symbolDown.getBitmap(), pt.measureLeft+pt.oldX, pt.measureUp+2*10);	
//				pt.oldX += symbolUp.getBitmap().getWidth() + pt.ct.SPACE;
//			}else{
//				//TODO
//			}
//			if (this.getTime().getSymbol() != null){
//				CommonSymbol id = null;
//				switch (this.getTime().getSymbol()){
//				case Common:
//					id = CommonSymbol.TimeCommon;
//					break;
//				case Cut:
//					break;
//				case SingleNumber:
//					break;
//				case Normal:
//					break;
//				}
//				Symbol symbol = pt.ct.symbolPool.getSymbol(id);
//				float y = pt.measureUp + 10 * 2 - symbol.getTopToBase();
//				pt.drawBitmap(symbol.getBitmap(), pt.measureLeft+pt.oldX, y);
//				pt.oldX = pt.oldX + symbol.getBitmap().getWidth() + pt.ct.SPACE;
//			}
//		}
		
	}
}