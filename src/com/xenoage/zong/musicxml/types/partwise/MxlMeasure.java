package com.xenoage.zong.musicxml.types.partwise;

import android.graphics.Paint.Align;
import android.util.Log;

import com.rojama.pianoshelf.PaintTransfer;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.MxlMusicData;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import com.xenoage.zong.symbols.Symbol;
import com.xenoage.zong.symbols.common.CommonSymbol;

import org.w3c.dom.Element;

@IncompleteMusicXML(partly = "measure-attributes", children = "music-data")
public final class MxlMeasure {
	public static final String ELEM_NAME = "measure";

	@NeverNull
	private final MxlMusicData musicData;

	@NeverNull
	private final String number;

	@NeverNull
	private final Float width;

	public MxlMeasure(MxlMusicData musicData, String number, Float width) {
		this.musicData = musicData;
		this.number = number;
		this.width = width;
	}

	@NeverNull
	public MxlMusicData getMusicData() {
		return this.musicData;
	}

	@NeverNull
	public String getNumber() {
		return this.number;
	}

	@NeverNull
	public Float getWidth() {
		return this.width;
	}

	@NeverNull
	public static MxlMeasure read(Element e) {
		return new MxlMeasure(MxlMusicData.read(e), InvalidMusicXML.throwNull(XMLReader.attribute(
				e, "number"), e), Parse.parseAttrFloatNull(e, "width"));
	}

	public void write(Element parent) {
		Element e = XMLWriter.addElement("measure", parent);
		this.musicData.write(e);
	}

	public void print(PaintTransfer pt) {
		//Log.d("Measure", pt.nowPartID + ":" + this.number);
		// TODO Auto-generated method stub
		pt.nowMeasure++;

		// 初始化参数
		// pt.nowLine = 1;
		// pt.lastNoteX = -1;
		pt.oldX = 0;
		pt.oldY = 0;
		// if (pt.measureUpAll.containsKey(1)){
		// pt.measureUp = pt.getMeasureUp(1);
		// }
		if (this.getWidth() != null) {
			pt.measureWidth = this.getWidth();
		} else {
			pt.measureWidth = 200;
		}
		this.musicData.print(pt);

		if (pt.nowPage != pt.ct.getDisPageNo() || pt.block)
			return;
		
		//Log.d("MeasureDone", pt.nowPartID + ":" + this.number + "/" + this.width + "/" + pt.nowLine + "/" + pt.measureUp);
		
		// pt.measureUp = pt.measureUpAll.get(pt.nowLine);
		for (Integer num : pt.nowClefType.keySet()) {
			pt.measureUp = pt.getMeasureUp(num);
			// 打五线谱
			for (int i = 0; i < 5; i++) {
				pt.drawLine(pt.measureLeft, pt.measureUp + i * 10,
						pt.measureLeft + pt.measureWidth, pt.measureUp + i * 10);
			}
			// pt.drawLine(pt.measureLeft + pt.measureWidth, pt.measureUp + 4 *
			// 10,
			// pt.measureLeft + pt.measureWidth, pt.measureUp);
		}

		// 记录首末行的Y坐标
		float firstY = pt.getMeasureUp(1), lastY = pt.getMeasureUp(pt.nowClefType.size()) + 4 * 10;

		// 画分割小节线
		pt.drawLine(pt.measureLeft + pt.measureWidth, firstY, pt.measureLeft + pt.measureWidth,
				lastY);
		if (pt.isNewSystem) {
			pt.drawLine(pt.measureLeft, firstY, pt.measureLeft, lastY);
		}

		// 每行先打印谱号和调号
		pt.oldX = 0;
		if (pt.isNewSystem) {
			// 如果是多行的打印大括号
			float midY = (firstY + lastY) / 2;
			float tempX = pt.measureLeft;
			if (pt.nowClefType.size() > 1) {
				Symbol symbol = pt.ct.symbolPool.getSymbol(CommonSymbol.BracketBrace);
				float y = midY - symbol.getTopToBase();
				tempX -= symbol.getBitmap().getWidth() + 5;
				pt.drawBitmap(symbol.getBitmap(), tempX, y);
			}
			// 第一节打印名称
			if (pt.nowMeasure == 1) {
				pt.getPaint().setTextAlign(Align.RIGHT);
				pt.getPaint().setTextSize(pt.ct.PART_NAME_SIZE);
				pt.drawText(pt.ct.scoreParts.get(pt.nowPartID).getName(), tempX - 15, midY + 5);
			}
			pt.printHand(false);
		}
		pt.isNewSystem = false;
		pt.measureLeft += pt.measureWidth;
	}
}