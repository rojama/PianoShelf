package com.rojama.pianoshelf;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Bitmap.Config;
import android.graphics.Path.FillType;
import android.graphics.PorterDuff.Mode;

import com.xenoage.pdlib.PVector;
import com.xenoage.zong.core.music.chord.Accidental;
import com.xenoage.zong.core.music.clef.ClefType;
import com.xenoage.zong.musicxml.types.MxlBeam;
import com.xenoage.zong.musicxml.types.MxlClef;
import com.xenoage.zong.musicxml.types.MxlCurvedLine;
import com.xenoage.zong.musicxml.types.MxlDefaults;
import com.xenoage.zong.musicxml.types.MxlNormalTime;
import com.xenoage.zong.musicxml.types.MxlNote;
import com.xenoage.zong.musicxml.types.MxlPageMargins;
import com.xenoage.zong.musicxml.types.MxlScorePart;
import com.xenoage.zong.musicxml.types.MxlStaffLayout;
import com.xenoage.zong.musicxml.types.MxlTime;
import com.xenoage.zong.musicxml.types.attributes.MxlPosition;
import com.xenoage.zong.musicxml.types.choice.MxlTimeContent.MxlTimeContentType;
import com.xenoage.zong.musicxml.types.enums.MxlMarginType;
import com.xenoage.zong.musicxml.types.enums.MxlStemValue;
import com.xenoage.zong.musicxml.types.groups.MxlAllMargins;
import com.xenoage.zong.symbols.Symbol;
import com.xenoage.zong.symbols.SymbolPool;
import com.xenoage.zong.symbols.common.CommonSymbol;

public class PaintTransfer {

	public CommonTransfer ct = null;

	public int nowDuration = 0;
	
	public float oldX = 0;
	public float oldY = 0;
	public float lastNoteX = -1; // 上一个音符的X坐标用于判断是否在同一个点上
	public Map<MxlBeam, PointF> lastBeamPoint = new HashMap<MxlBeam, PointF>();
	public Map<MxlCurvedLine, PointF> lastCurvedLinePoint = new HashMap<MxlCurvedLine, PointF>();;
	public String nowPartID;
	public int nowPage = 1;
	public int nowLine = 0;   // 初始 0（MxlPrint L142 里 ++nowLine → 第 1 行 key=1）
	public int nowMeasure = 0;

	public float measureLeft = 0; // 当前小节左边X坐标
	public float measureUp = 0; // 当前小节上边Y坐标
	public float measureWidth = 0;
	public Map<Integer, Float> measureUpAll = new HashMap<Integer, Float>();
	public Map<Integer, ClefType> nowClefType = new HashMap<Integer, ClefType>();
	public Map<Integer, Float> staffLayout = new HashMap<Integer, Float>();
	public int nowFifths;
	public MxlTime nowTime;
	public int divisions;
	public boolean isNewSystem;

	// 用于控制多Part的跳转
	public boolean block = false;
	public boolean firstIn = true;
	// public Map<String, Integer> oldMeasure = new HashMap<String, Integer>();
	// //已经处理过的小节
	// public Map<String, Integer> oldLine = new HashMap<String, Integer>();
	// public Map<String, Integer> oldPage = new HashMap<String, Integer>();
	public Map<String, PaintTransfer> oldPaintTransfer = new HashMap<String, PaintTransfer>();

	public Float getMeasureUp(Integer num) {
		int ln = (nowLine < 1) ? 1 : nowLine;
		Float raw = this.measureUpAll.get(ln);
		// ===== NPE 兜底：measureUpAll 中不存在 nowLine 对应 key 时，按 systemTopDistance + (ln-1)*systemDistance 推算 =====
		// 正常流程 measureUpAll 应由 MxlPrint 在 collect/render 阶段写入，这里防极端 case（如 XML 缺 print/new-system 节点）
		if (raw == null) {
			int staffH = (ct != null) ? (4 * ct.STAFF_LINE_SPACING) : 40;
			// systemDistance / systemTopDistance 都是 primitive float，默认 0；> 0 代表 XML 有写入
			float sysDist = (ct != null && ct.systemDistance > 0) ? ct.systemDistance : staffH * 2f;
			float topDist = (ct != null && ct.systemTopDistance > 0) ? ct.systemTopDistance : staffH * 2f;
			// 页边距：取 pagemargins 第 1 个的 top（默认 0）
			float topMargin = 0f;
			if (ct != null && ct.pagemargins != null && ct.pagemargins.size() > 0
					&& ct.pagemargins.get(0) != null) {
				topMargin = ct.pagemargins.get(0).getTopMargin();
				if (topMargin < 0) topMargin = 0f;
			}
			float baseY = topDist + topMargin;
			// nowLine=1 → 不加系统距离；nowLine>=2 → 每条 line 叠加系统距离+行高(单 staff 近似)
			float fallback = baseY + Math.max(0, ln - 1) * (sysDist + staffH);
			// 写入缓存，避免后续同一 line 重算（保持 nowPage=disPageNo 的渲染时一致）
			this.measureUpAll.put(ln, fallback);
			raw = fallback;
		}
		float meaup = raw;
		int staffH = (ct != null) ? (4 * ct.STAFF_LINE_SPACING) : 40;
		// staff 叠加：num=第几个 staff（不是总 staff 数），加 staff 间距 + 每个 staff 高
		for (int i = 1; i < num; i++) {
			Float d = getStaffDistance(i + 1);
			meaup += (d != null ? d : (staffH * 1.5f)) + staffH;
		}
		return meaup;
	}

	public Float getStaffDistance(Integer num) {
		if (this.staffLayout.containsKey(num)) {
			Float v = this.staffLayout.get(num);
			// XML staff-distance 是 tenths 单位 → 转换为 px（paint 时 tenthsToPx 已就绪）
			if (v != null && ct != null && ct.tenthsToPx > 0f) {
				return v * ct.tenthsToPx;
			}
			return v;
		} else if (this.staffLayout.containsKey(null)) {
			Float v = this.staffLayout.get(null);
			if (v != null && ct != null && ct.tenthsToPx > 0f) return v * ct.tenthsToPx;
			return v;
		} else if (this.ct != null && this.ct.systemDistance > 0) {
			// 注意：systemDistance 已经是 px（在 SystemLayout.paint 里 *过 tenthsToPx）
			return this.ct.systemDistance;
		} else {
			// 终极兜底：返回 1.5 倍行高（避免 null 导致调用方拆箱 NPE）
			return (ct != null) ? (1.5f * 4 * ct.STAFF_LINE_SPACING) : 60f;
		}
	}

	public void initNow() {
		oldX = 0;
		oldY = 0;
		nowPartID = "";
		nowPage = 1;
		nowLine = 1;
		nowMeasure = 0;
		nowDuration = 0;
		measureLeft = 0;
		measureUp = 0;
		measureWidth = 0;
		measureUpAll.clear();
		nowClefType.clear();
		staffLayout.clear();
		// divisions 兜底默认 16（MusicXML 里最常见的默认值：
		// 1 quarter note = 16 divisions；若 XML 有 <attributes><divisions> 会随后覆盖）
		// 避免 MxlNote.print 里 nowDuration*64/divisions 除零导致 duration/type 全算错。
		divisions = 16;
		block = false;
		firstIn = true;
		// oldMeasure.clear(); //已经处理过的小节
		// oldLine.clear();
		// oldPage.clear();
	}

	// 取当前页的Margins
	public MxlAllMargins getMxlAllMargins() {
		for (MxlPageMargins mpm : ct.pagemargins) {
			if (mpm.getType() == MxlMarginType.Both) {
				return mpm.getValue();
			}
			if (mpm.getType() == MxlMarginType.Even && nowPage % 2 == 0) {
				return mpm.getValue();
			}
			if (mpm.getType() == MxlMarginType.Odd && nowPage % 2 != 0) {
				return mpm.getValue();
			}
		}
		return new MxlAllMargins(0, 0, 0, 0);
	}

	public Point getPointFromMxlPosition(MxlPosition pos) {
		float x = 0, y = 0;
		if (pos.getDefaultX() != null) {
			x = pos.getDefaultX();
		} else if (pos.getRelativeX() != null) {
			x = oldX + pos.getRelativeX();
		}
		if (pos.getDefaultY() != null) {
			y = pos.getDefaultY();
		} else if (pos.getRelativeY() != null) {
			y = oldY + pos.getRelativeY();
		}
		this.setPoint(x, y);

		return new Point(Math.round(x), Math.round(this.ct.pageHeight - y));
		// return new Point(this.getZoomedX(x),this.getZoomedY(ct.pageHeight -
		// y));
	}

	public void setPointInMeasure(MxlPosition pos) {
		if (pos.getDefaultX() != null) {
			oldX = pos.getDefaultX();
		} else if (pos.getRelativeX() != null) {
			oldX = oldX + pos.getRelativeX();
		}

		if (pos.getDefaultY() != null) {
			oldY = -pos.getDefaultY();
		} else if (pos.getRelativeY() != null) {
			oldY = oldY - pos.getRelativeY();
		}
		return;
	}

	// 绘图公用：统一按 ct.SYMBOL_SCALE 缩放（保证音符大小与五线谱间距成比例），
	// 然后对 bitmap 按当前画笔颜色 MULTIPLY 染色（保证前景色统一，例如夜间模式下变白）。
	public void drawBitmap(Bitmap bitmap, float left, float top) {
		// collect 阶段只收集 note 坐标，不执行任何绘制（避免 symbol bitmap 被染色修改、canvas 被污染）
		if (ct != null && ct.collectAllNotesForPlayback) return;
		Bitmap src = bitmap;
		if (src == null) return;
		float s = (ct != null) ? ct.SYMBOL_SCALE : 1f;
		if (s != 1f) {
			try {
				Matrix m = new Matrix();
				m.postScale(s, s);
				src = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
			} catch (Throwable ignored) {
				src = bitmap;
			}
		}
		// 染色：按当前 paint.getColor() MULTIPLY 叠色
		try {
			Canvas can = new Canvas(src);
			can.drawColor(this.getPaint().getColor(), Mode.MULTIPLY);
		} catch (Throwable ignored) { /* 某些 bitmap config 不支持可写 */ }
		this.getCanvas().drawBitmap(src, left, top, this.getPaint());
	}

	/** 按 SYMBOL_SCALE 缩放后的 symbol bitmap width / height / topToBase，避免调用者自己写 math。 */
	public int symW(Symbol symbol) {
		if (symbol == null || symbol.getBitmap() == null) return 0;
		return Math.max(1, Math.round(symbol.getBitmap().getWidth() * symScale()));
	}
	public int symH(Symbol symbol) {
		if (symbol == null || symbol.getBitmap() == null) return 0;
		return Math.max(1, Math.round(symbol.getBitmap().getHeight() * symScale()));
	}
	public int symTopToBase(Symbol symbol) {
		if (symbol == null) return 0;
		return Math.round(symbol.getTopToBase() * symScale());
	}
	private float symScale() {
		return (ct != null) ? ct.SYMBOL_SCALE : 1f;
	}
	private int S() { return (ct != null) ? ct.STAFF_LINE_SPACING : 10; }

	public void drawText(String textContent, float x, float y) {
		if (ct != null && ct.collectAllNotesForPlayback) return;
		this.getCanvas().drawText(textContent, x, y, this.getPaint());
	}

	public void drawLine(float startX, float startY, float stopX, float stopY) {
		if (ct != null && ct.collectAllNotesForPlayback) return;
		this.getCanvas().drawLine(startX, startY, stopX, stopY, this.getPaint());
	}

	public void drawPath(Path path) {
		if (ct != null && ct.collectAllNotesForPlayback) return;
		this.getCanvas().drawPath(path, this.getPaint());
	}

	public void drawBezierPath(PointF start, PointF control, PointF end) {
		Path path = new Path();
		path.reset();
		path.moveTo(start.x, start.y);
		path.quadTo(control.x, control.y, end.x, end.y);
		this.drawPath(path);
		path.reset();
	}

	public void drawBezierPath(PointF start, PointF controlLow, PointF controlHig, PointF end) {
		Path path = new Path();
		path.reset();
		path.moveTo(start.x, start.y);
		path.quadTo(controlLow.x, controlLow.y, end.x, end.y);
		path.quadTo(controlHig.x, controlHig.y, start.x, start.y);
		this.drawPath(path);
		path.reset();
	}

	public void drawDefaultBezier(PointF start, PointF end, MxlStemValue sv) {
		PointF controlLow = new PointF();
		PointF controlHig = new PointF();
		int sp = S();
		int off10 = sp;                  // 原 10
		int off15 = Math.round(sp * 1.5f); // 原 15
		int off20 = Math.round(sp * 2.0f); // 原 20
//		if (start.x <= end.x) {
			switch (sv) {
			default:
			case Up:
				start.y += off10;
				end.y += off10;
				controlLow.set((start.x + end.x) / 2, (start.y + end.y) / 2 + off15);
				controlHig.set((start.x + end.x) / 2, (start.y + end.y) / 2 + off20);
				break;
			case Down:
				start.y -= off10;
				end.y -= off10;
				controlLow.set((start.x + end.x) / 2, (start.y + end.y) / 2 - off15);
				controlHig.set((start.x + end.x) / 2, (start.y + end.y) / 2 - off20);
				break;
			}
			this.drawBezierPath(start, controlLow, controlHig, end);
//		} else {
//			PointF startM = new PointF();
//			startM.set(start);
//			startM.x = getPageWidth() - getMxlAllMargins().getRightMargin();
//			startM.y += ((sv == MxlStemValue.Up) ? 15 : -15);
//			controlLow.set(startM.x, (start.y + startM.y) / 2 + ((sv == MxlStemValue.Up) ? 15 : -15));
//			controlHig.set(startM.x, (start.y + startM.y) / 2 + ((sv == MxlStemValue.Up) ? 20 : -20));
//			this.drawBezierPath(start, controlLow, controlHig, startM);
//
//			PointF endM = new PointF();
//			endM.set(end);
//			endM.x = getMxlAllMargins().getLeftMargin();
//			endM.y += ((sv == MxlStemValue.Up) ? 15 : -15);
//			controlLow.set(endM.x, (endM.y + end.y) / 2 + ((sv == MxlStemValue.Up) ? 15 : -15));
//			controlHig.set(endM.x, (endM.y + end.y) / 2 + ((sv == MxlStemValue.Up) ? 20 : -20));
//			this.drawBezierPath(endM, controlLow, controlHig, end);
//		}
	}

	public void printHand(boolean printTimeOnly) {
		// collect 阶段不 return：printClef/printKey/printTime 内部 drawBitmap 会被 collect 模式 short-circuit
		// 但 getMeasureUp(line) 仍然需要执行（间接依赖 measureUpAll），并且 oldX 推进也要正常完成
		if (!ct.collectAllNotesForPlayback && this.nowPage != this.ct.getDisPageNo())
			return;

		float w = 0;
		int line = 1;
		if (this.oldX == 0)
			this.oldX += this.ct.SPACE;
		float tempX = this.oldX;
		for (int key : this.nowClefType.keySet()) {
			this.oldX = tempX;
			if (!printTimeOnly) {
				w = printClef(key, this.oldX, this.getMeasureUp(line));
				this.oldX = this.oldX + w + this.ct.SPACE;
				w = printKey(this.oldX, this.getMeasureUp(line));
				this.oldX = this.oldX + w + this.ct.SPACE;
			}
			w = printTime(this.oldX, this.getMeasureUp(line));
			this.oldX = this.oldX + w + this.ct.SPACE;
			line++;
		}
	}

	// 画谱号
	public float printClef(int key, float x, float y) {
		ClefType clef = this.nowClefType.get(key);
		if (this.nowClefType != null && clef != null) {
			CommonSymbol id = null;
			if (clef == ClefType.G) {
				id = CommonSymbol.getClef(ClefType.G);
			} else if (clef == ClefType.F) {
				id = CommonSymbol.getClef(ClefType.F);
			} else {
				id = CommonSymbol.getClef(ClefType.G); // C / Percussion / TAB / None 兜底 G
			}
			Symbol symbol = this.ct.symbolPool.getSymbol(id);
			if (symbol == null || symbol.getBitmap() == null) return 0;
			int line = clef.getLine();
			int sp = S();
			y = y - line * sp / 2;
			y += 4 * sp - symTopToBase(symbol);
			this.drawBitmap(symbol.getBitmap(), this.measureLeft + x, y);
			return symW(symbol);
		}
		return 0;
	}

	// 画调号
	public float printKey(float x, float y) {
		Accidental.Type id = null;
		switch (this.nowFifths) {
		case 4:
		case 3:
		case 2:
			id = Accidental.Type.DoubleSharp;
			break;
		case 1:
			id = Accidental.Type.Sharp;
			break;
		case 0:
			id = Accidental.Type.Natural;
			break;
		case -1:
			id = Accidental.Type.Flat;
			break;
		case -2:
		case -3:
		case -4:
			id = Accidental.Type.DoubleFlat;
		}
		if (id != null) {
			Symbol symbol = ct.symbolPool.getSymbol(CommonSymbol.getAccidental(id));
			if (symbol == null || symbol.getBitmap() == null) return 0;
			y = y + S() * 2 - symTopToBase(symbol);
			this.drawBitmap(symbol.getBitmap(), this.measureLeft + x, y);
			return symW(symbol);
		}
		return 0;
	}

	// 画节拍
	public float printTime(float x, float y) {
		float width = 0;
		if (this.nowTime != null) {
			if (this.nowTime.getContent().getTimeContentType() == MxlTimeContentType.NormalTime) {
				MxlNormalTime timecon = (MxlNormalTime) this.nowTime.getContent();
				Symbol symbolUp = ct.symbolPool
						.getSymbol(CommonSymbol.getDigit(timecon.getBeats()));
				Symbol symbolDown = ct.symbolPool.getSymbol(CommonSymbol.getDigit(timecon
						.getBeatType()));
				if (symbolUp != null) {
					this.drawBitmap(symbolUp.getBitmap(), this.measureLeft + x, y + 1);
					x += symW(symbolUp) + ct.SPACE;
					width += symW(symbolUp);
				}
				if (symbolDown != null) {
					this.drawBitmap(symbolDown.getBitmap(), this.measureLeft + x - (symbolUp == null ? 0 : symW(symbolUp)) - ct.SPACE, y + 2 * S() + 1);
				}
			} else {
				// TODO
			}
		}
		return width;
	}

	public void setPoint(float x, float y) {
		// Log.d("SetPoint", x + ":" + y);
		oldX = x;
		oldY = y;
	}

	public float getX() {
		return this.oldX;
	}

	public float getCanvasX() {
		return this.oldX;
	}

	public float getY() {
		return this.oldY;
	}

	// 原点在左下角转成左上角
	public float getCanvasY() {
		return ct.pageHeight - this.oldY;
	}

	public Canvas getCanvas() {
		return ct.canvas;
	}

	public void setCanvas(Canvas canvas) {
		this.ct.canvas = canvas;
	}

	public Paint getPaint() {
		return ct.paint;
	}

	public void setPaint(Paint paint) {
		this.ct.paint = paint;
	}

	public boolean isUpright() {
		return ct.isUpright;
	}

	public void setUpright(boolean isUpright) {
		this.ct.isUpright = isUpright;
	}

	public int getScreenWidth() {
		return ct.screenWidth;
	}

	public void setScreenWidth(int screenWidth) {
		this.ct.screenWidth = screenWidth;
	}

	public int getScreenHeight() {
		return ct.screenHeight;
	}

	public void setScreenHeight(int screenHeight) {
		this.ct.screenHeight = screenHeight;
	}

	public float getPageWidth() {
		return ct.pageWidth;
	}

	public void setPageWidth(float pageWidth) {
		this.ct.pageWidth = pageWidth;
	}

	public float getPageHeight() {
		return ct.pageHeight;
	}

	public void setPageHeight(float pageHeight) {
		ct.pageHeight = pageHeight;
	}

}
