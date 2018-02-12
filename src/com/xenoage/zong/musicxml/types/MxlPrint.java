package com.xenoage.zong.musicxml.types;

import android.graphics.Paint.Align;
import android.util.Log;

import com.rojama.pianoshelf.PaintTransfer;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.core.music.clef.ClefType;
import com.xenoage.zong.musicxml.types.attributes.MxlPrintAttributes;
import com.xenoage.zong.musicxml.types.choice.MxlMusicDataContent;
import com.xenoage.zong.musicxml.types.enums.MxlClefSign;
import com.xenoage.zong.musicxml.types.groups.MxlLayout;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.symbols.Symbol;
import com.xenoage.zong.symbols.common.CommonSymbol;

import org.w3c.dom.Element;

@IncompleteMusicXML(missing = "measure-layout,measure-numbering,part-name-display,part-abbreviation-display", children = "layout,print-attributes", partly = "")
public final class MxlPrint implements MxlMusicDataContent {
	public static final String ELEM_NAME = "print";
	public static final MxlPrint empty = new MxlPrint(MxlLayout.empty, MxlPrintAttributes.empty);

	@NeverNull
	private final MxlLayout layout;

	@NeverNull
	private final MxlPrintAttributes printAttributes;

	public MxlPrint(MxlLayout layout, MxlPrintAttributes printAttributes) {
		this.layout = layout;
		this.printAttributes = printAttributes;
	}

	@NeverNull
	public MxlLayout getLayout() {
		return this.layout;
	}

	@NeverNull
	public MxlPrintAttributes getPrintAttributes() {
		return this.printAttributes;
	}

	@Override
	public MxlMusicDataContent.MxlMusicDataContentType getMusicDataContentType() {
		return MxlMusicDataContent.MxlMusicDataContentType.Print;
	}

	@NeverNull
	public static MxlPrint read(Element e) {
		MxlLayout layout = MxlLayout.read(e);
		MxlPrintAttributes printAttributes = MxlPrintAttributes.read(e);
		if ((layout != MxlLayout.empty) || (printAttributes != MxlPrintAttributes.empty)) {
			return new MxlPrint(layout, printAttributes);
		}
		return empty;
	}

	@Override
	public void write(Element parent) {
		if (this != empty) {
			Element e = XMLWriter.addElement("print", parent);
			this.layout.write(e);
			this.printAttributes.write(e);
		}
	}

	@Override
	public void print(PaintTransfer pt) {
		// 如果有换行判断是否要处理
		if (this.getPrintAttributes() != null) {
			if (this.getPrintAttributes().getNewPage() != null) {
				if (this.getPrintAttributes().getNewPage()) {
					if (!pt.firstIn) {
						pt.block = true;
						pt.nowMeasure--;
						return;
					}
				}
			}
			if (this.getPrintAttributes().getNewSystem() != null) {
				if (this.getPrintAttributes().getNewSystem()) {
					if (!pt.firstIn) {
						pt.block = true;
						pt.nowMeasure--;
						return;
					}
				}
			}
		}

		// boolean isNewSystem = false;
		if (this.getPrintAttributes() != null) {
			if (this.getPrintAttributes().getNewPage() != null) {
				if (this.getPrintAttributes().getNewPage()) {
					pt.nowPage++;
					if (pt.ct.maxPage < pt.nowPage)
						pt.ct.maxPage = pt.nowPage;
					pt.ct.oldPartID = null;
					pt.measureLeft = pt.ct.systemLeftMargin + pt.getMxlAllMargins().getLeftMargin();
					pt.measureUp = pt.ct.systemTopDistance + pt.getMxlAllMargins().getTopMargin();
					pt.isNewSystem = true;
				}
			}
		}
		if (this.getLayout() != null)
			this.getLayout().paint(pt);
		if (this.getPrintAttributes() != null) {
			if (this.getPrintAttributes().getNewSystem() != null) {
				if (this.getPrintAttributes().getNewSystem()) {
				//	pt.nowLine++;
//					if (pt.measureUpAll.containsKey(pt.nowLine)) {
//						pt.measureUp = pt.getMeasureUp(1);
//					} else {
//						pt.measureUp += pt.ct.systemDistance + 40;
//					}
					pt.measureLeft = pt.ct.systemLeftMargin + pt.getMxlAllMargins().getLeftMargin();
					pt.isNewSystem = true;
				}
			}
		}

		pt.oldX = 0;
		pt.oldY = 0;

		if (pt.nowPage != pt.ct.getDisPageNo())
			return;

		// 第一部分的第一节
		if (pt.nowMeasure == 1) {
			pt.measureUp = pt.ct.systemTopDistance + pt.getMxlAllMargins().getTopMargin();
			pt.measureLeft = pt.ct.systemLeftMargin + pt.getMxlAllMargins().getLeftMargin();
			pt.isNewSystem = true;
		}

		// 记录每一行的顶部用于下一部分的测量
		if (pt.isNewSystem || pt.measureUpAll.isEmpty()) {
			if (pt.ct.oldPartID != null) {
				Log.d("pt.nowLine", pt.nowLine+"");
				PaintTransfer oldPT = pt.ct.oldPaintTransfer.get(pt.ct.oldPartID);
				pt.measureUp = oldPT.getMeasureUp(oldPT.nowClefType.size()) + pt.getStaffDistance(1) + 40;
			}
//			else{
//				if (pt.measureUpAll.containsKey(pt.nowLine-1)){
//					pt.measureUp = pt.measureUpAll.get(pt.nowLine-1) + pt.ct.systemDistance + 40;
//				}
//			}
			pt.measureUpAll.put(++pt.nowLine, pt.measureUp);
		}

		// if (!pt.firstIn && pt.isNewSystem) {
		// pt.block = true;
		// // pt.nowMeasure--; // 判断时已经加1，所以要恢复
		// // pt.nowLine--;
		// } else {
		// pt.block = false;
		// }

		// // 第一节打印名称
		// if (pt.nowMeasure == 1) {
		// pt.getPaint().setTextAlign(Align.RIGHT);
		// pt.getPaint().setTextSize(pt.PART_NAME_SIZE);
		// pt.drawText(pt.ct.scoreParts.get(pt.nowPartID).getName(),
		// pt.measureLeft - 10,
		// pt.measureUp + 2 * 10);
		// }
		// else{
		// //之后每行先打印谱号和调号
		// if (isNewSystem) {
		// pt.printHand(false);
		// }
		// }
	}
}