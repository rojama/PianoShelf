package com.rojama.pianoshelf;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
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
	public int nowLine = 1;
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
		float meaup = this.measureUpAll.get(nowLine);
		// meaup += this.getStaffDistance(1) + 40;
		for (int i = 1; i < num; i++) {
			meaup += getStaffDistance(i + 1) + 40;
		}
		return meaup;
	}

	public Float getStaffDistance(Integer num) {
		if (this.staffLayout.containsKey(num)) {
			return this.staffLayout.get(num);
		} else if (this.staffLayout.containsKey(null)) {
			return this.staffLayout.get(null);
		} else {
			return this.ct.systemDistance;
		}
	}

	public void initNow() {
		oldX = 0;
		oldY = 0;
		nowPartID = "";
		nowPage = 1;
		nowLine = 1;
		nowMeasure = 0;
		measureLeft = 0;
		measureUp = 0;
		measureWidth = 0;
		measureUpAll.clear();
		nowClefType.clear();
		staffLayout.clear();
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

	// 绘图公用
	public void drawBitmap(Bitmap bitmap, float left, float top) {
		Canvas can = new Canvas(bitmap);
		can.drawColor(this.getPaint().getColor(), Mode.MULTIPLY);
		this.getCanvas().drawBitmap(bitmap, left, top, this.getPaint());
	}

	public void drawText(String textContent, float x, float y) {
		this.getCanvas().drawText(textContent, x, y, this.getPaint());
	}

	public void drawLine(float startX, float startY, float stopX, float stopY) {
		this.getCanvas().drawLine(startX, startY, stopX, stopY, this.getPaint());
	}

	public void drawPath(Path path) {
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
//		if (start.x <= end.x) {
			switch (sv) {
			default:
			case Up:
				start.y += 10;
				end.y += 10;
				controlLow.set((start.x + end.x) / 2, (start.y + end.y) / 2 + 15);
				controlHig.set((start.x + end.x) / 2, (start.y + end.y) / 2 + 20);
				break;
			case Down:
				start.y -= 10;
				end.y -= 10;
				controlLow.set((start.x + end.x) / 2, (start.y + end.y) / 2 - 15);
				controlHig.set((start.x + end.x) / 2, (start.y + end.y) / 2 - 20);
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
		if (this.nowPage != this.ct.getDisPageNo())
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
		if (this.nowClefType != null) {
			CommonSymbol id = null;
			if (clef == ClefType.G) {
				id = CommonSymbol.getClef(ClefType.G);
			} else if (clef == ClefType.F) {
				id = CommonSymbol.getClef(ClefType.F);
			}
			Symbol symbol = this.ct.symbolPool.getSymbol(id);
			int line = clef.getLine();
			y = y - line * 10 / 2;
			y += 4 * 10 - symbol.getTopToBase();
			this.drawBitmap(symbol.getBitmap(), this.measureLeft + x, y);
			return symbol.getBitmap().getWidth();
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
			y = y + 10 * 2 - symbol.getTopToBase();
			this.drawBitmap(symbol.getBitmap(), this.measureLeft + x, y);
			return symbol.getBitmap().getWidth();
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
				this.drawBitmap(symbolUp.getBitmap(), this.measureLeft + x, y + 1);
				this.drawBitmap(symbolDown.getBitmap(), this.measureLeft + x, y + 2 * 10 + 1);
				x += symbolUp.getBitmap().getWidth() + ct.SPACE;
				width += symbolUp.getBitmap().getWidth();
			} else {
				// TODO
			}
			// if (this.nowTime.getSymbol() != null) {
			// CommonSymbol id = null;
			// switch (this.nowTime.getSymbol()) {
			// case Common:
			// id = CommonSymbol.TimeCommon;
			// break;
			// case Cut:
			// break;
			// case SingleNumber:
			// break;
			// case Normal:
			// break;
			// }
			// Symbol symbol = ct.symbolPool.getSymbol(id);
			// y = y + 10 * 2 - symbol.getTopToBase();
			// this.drawBitmap(symbol.getBitmap(), this.measureLeft + x, y);
			// width += symbol.getBitmap().getWidth();
			// }
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
