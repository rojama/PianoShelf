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
		// ===== 修复 (1): 当 XML 没有 <print new-page>，但下一 system 预测 Y > pageBottom 时，自动分页 =====
		// (scoreHeader.paint → layout.paint 已跑完，pt.ct.pageHeight/pageWidth/tenthsToPx 都已就绪)
		// 先尝试：当 printAttributes.getNewSystem()/getNewPage() 为 null，但 nowMeasure>=1 时，
		// 检查 nowLine 的 measureUp（第 1 staff）+ 所有 staff 叠加总高 + systemDistance 是否超 bottomMargin。
		try {
			int numStaffs = pt.nowClefType.size();
			if (numStaffs <= 0) numStaffs = Math.max(1, pt.staffLayout.size());
			final int SP = pt.ct.STAFF_LINE_SPACING;
			// 计算当前页面最后一个 system 的预测底 Y（若 isNewSystem 触发，将落在 nowLine 的下一行）
			int nextLineKey = pt.nowLine + 1;
			float nextSystemTopY = 0f;
			// 如果 nowMeasure > 1 且还没有 measureUpAll 下一条，推算：
			if (pt.nowMeasure > 0 && !pt.firstIn) {
				float lastTopY;
				if (pt.measureUpAll.containsKey(nextLineKey)) {
					lastTopY = pt.measureUpAll.get(nextLineKey);
				} else {
					// 基于 nowLine 的 getMeasureUp(1) + systemDistance（已经 px 化）
					float cur1 = pt.getMeasureUp(1);
					float sysDist = (pt.ct.systemDistance > 0) ? pt.ct.systemDistance
							: (SP * 4 + 24f);
					lastTopY = cur1 + sysDist;
				}
				// 总 staff 高：staff 1 topY + (numStaffs-1) staffs 各自 getStaffDistance + 最后 staff 五线谱高
				float totalSysBottomY = lastTopY; // 先给 staff 1 top
				for (int s = 2; s <= numStaffs; s++) {
					Float dist = pt.getStaffDistance(s);
					totalSysBottomY += (dist != null ? dist.floatValue() : (SP * 1.5f));
				}
				totalSysBottomY += 4f * SP; // staff 五线谱 5 线 4 间隔

				// 页面可用底部
				float bottomMarginPx = 0f;
				if (pt.ct.pagemargins != null && pt.ct.pagemargins.size() > 0
						&& pt.ct.pagemargins.get(0) != null) {
					bottomMarginPx = pt.ct.pagemargins.get(0).getBottomMargin();
					if (pt.ct.tenthsToPx > 0f) bottomMarginPx *= pt.ct.tenthsToPx;
				} else {
					bottomMarginPx = 4 * SP;
				}
				float availBottom = pt.ct.pageHeight - bottomMarginPx;

				// 当这个是 new-system 的 print（XML 显式写了）或 即便没写但 measureLeft+measureWidth
				// 已经接近右边（要换行的隐含条件），且超过 bottom，就触发 nowPage++
				boolean forcePage = (totalSysBottomY > availBottom) && pt.nowMeasure > 1;
				// XML 里显式 new-system 但没有 new-page 标记时也判断
				boolean explicitNewSys = this.getPrintAttributes() != null
						&& this.getPrintAttributes().getNewSystem() != null
						&& this.getPrintAttributes().getNewSystem();
				if (forcePage || explicitNewSys && totalSysBottomY > availBottom * 0.98f) {
					pt.nowPage++;
					if (pt.ct.maxPage < pt.nowPage) pt.ct.maxPage = pt.nowPage;
					pt.ct.oldPartID = null;
					pt.nowLine = 0;
					// 下面的 new-page 分支会继续处理 measureLeft / isNewSystem
					if (this.getPrintAttributes() == null
							|| this.getPrintAttributes().getNewPage() == null
							|| !this.getPrintAttributes().getNewPage()) {
						// 用临时方式模拟 new-page
						float topMarginPx = 0f;
						if (pt.ct.pagemargins != null && pt.ct.pagemargins.size() > 0
								&& pt.ct.pagemargins.get(0) != null) {
							topMarginPx = pt.ct.pagemargins.get(0).getTopMargin();
							if (pt.ct.tenthsToPx > 0f) topMarginPx *= pt.ct.tenthsToPx;
						}
						float leftMarginPx = 0f;
						if (pt.ct.pagemargins != null && pt.ct.pagemargins.size() > 0
								&& pt.ct.pagemargins.get(0) != null) {
							leftMarginPx = pt.ct.pagemargins.get(0).getLeftMargin();
							if (pt.ct.tenthsToPx > 0f) leftMarginPx *= pt.ct.tenthsToPx;
						}
						pt.measureLeft = pt.ct.systemLeftMargin + leftMarginPx;
						pt.measureUp = pt.ct.systemTopDistance + topMarginPx;
						pt.isNewSystem = true;
						com.rojama.pianoshelf.DebugLog.d("AutoPageBreak",
								"触发自动分页: " +
								"nowPage=" + pt.nowPage +
								" 预测 nextSystemBottomY=" + totalSysBottomY +
								"  可用 bottom=" + availBottom +
								"  maxPage=" + pt.ct.maxPage);
					}
				}
			}
		} catch (Throwable t) {
			com.rojama.pianoshelf.DebugLog.w("AutoPageBreak", "分页预判出错（忽略，继续渲染）", t);
		}

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
					// tenths → px margins 换算
					float leftM = pt.getMxlAllMargins().getLeftMargin();
					float topM = pt.getMxlAllMargins().getTopMargin();
					if (pt.ct.tenthsToPx > 0f) { leftM *= pt.ct.tenthsToPx; topM *= pt.ct.tenthsToPx; }
					pt.measureLeft = pt.ct.systemLeftMargin + leftM;
					pt.measureUp = pt.ct.systemTopDistance + topM;
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
					float leftM = pt.getMxlAllMargins().getLeftMargin();
					if (pt.ct.tenthsToPx > 0f) leftM *= pt.ct.tenthsToPx;
					pt.measureLeft = pt.ct.systemLeftMargin + leftM;
					pt.isNewSystem = true;
				}
			}
		}

		pt.oldX = 0;
		pt.oldY = 0;

		// ===== 修复：collect 阶段不 return =====
		// measureUpAll.put 在这里（line ~150）执行，return 掉会导致 measureUpAll 为空
		// 然后 MxlNote.print -> getMeasureUp(getStaff()) 得到 null -> Float 拆箱 NPE
		if (!pt.ct.collectAllNotesForPlayback && pt.nowPage != pt.ct.getDisPageNo())
			return;

		// 第一部分的第一节
		if (pt.nowMeasure == 1) {
			float topM = pt.getMxlAllMargins().getTopMargin();
			float leftM = pt.getMxlAllMargins().getLeftMargin();
			if (pt.ct.tenthsToPx > 0f) {
				topM *= pt.ct.tenthsToPx;
				leftM *= pt.ct.tenthsToPx;
			}
			pt.measureUp = pt.ct.systemTopDistance + topM;
			pt.measureLeft = pt.ct.systemLeftMargin + leftM;
			pt.isNewSystem = true;
		}

		// 记录每一行的顶部用于下一部分的测量
		if (pt.isNewSystem || pt.measureUpAll.isEmpty()) {
			if (pt.ct.oldPartID != null) {
				android.util.Log.d("pt.nowLine", pt.nowLine+"");
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