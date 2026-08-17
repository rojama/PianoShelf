package com.rojama.pianoshelf;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;
import android.util.Log;

import com.xenoage.pdlib.PVector;
import com.xenoage.zong.musicxml.types.MxlDefaults;
import com.xenoage.zong.musicxml.types.MxlNote;
import com.xenoage.zong.musicxml.types.MxlPageMargins;
import com.xenoage.zong.musicxml.types.MxlScorePart;
import com.xenoage.zong.symbols.SymbolPool;

public class CommonTransfer {
	private static final String TAG = "CommonTransfer";

	public final int PART_NAME_SIZE = 15;

	/**
	 * MusicXML tenths → 像素 换算系数（由 <defaults><millimeters>/<tenths> 计算）。
	 *   默认 6.35mm / 40tenths = 0.15875 mm/tenth，再按 DPI 换算为 px。
	 * XML 里所有距离（staff-distance / system-distance / page-width / page-height /
	 *        staff-distance 等）都要乘以这个系数，才能得到正确的像素尺寸。
	 * XML 未声明时用默认值。
	 */
	public float tenthsToPx = 0f;   // 在 MxlDefaults.paint() 中初始化
	public float mmToPx = 1f / 3.528f;  // 默认 90 DPI → 1mm≈3.528px，1/3.528≈0.2835（兜底值）

	/**
	 * 五线谱"两线之间"的距离（1 staff-space）。
	 * 【由 XML tenths 动态计算，不再写死 9】
	 *   XML defaults: 40 tenths = 6.35mm → 1 staff-space = 10 tenths（标准 MusicXML）
	 *   → STAFF_LINE_SPACING = 10 * tenthsToPx；
	 * 如果 XML 没有显式给出 staff-layout/staff-distance，则使用该计算值。
	 */
	public int STAFF_LINE_SPACING = 10;   // 默认 10（兜底），paint 中会覆盖

	/** 符干长度 = 约 3.5 个 STAFF_LINE_SPACING（标准音乐出版规范：3.5 space） */
	public int NOTE_LINE_HIGHT = 35;

	/** 音符之间水平间隔（≈ 半个 staff-space，按 STAFF_LINE_SPACING 计算） */
	public int SPACE = 5;

	/** 单个 note head 大致宽度（≈ 1.4× staff-space），用于 chord/X 位置判断 */
	public int NOTE_WIDTH = 14;

	/**
	 * Symbol bitmap 缩放因子。
	 * 默认符号按 STAFF_LINE_SPACING=10（10px）设计；如果实际 STAFF_LINE_SPACING 为 16px，
	 * 应该放大 1.6 倍 → SYMBOL_SCALE = STAFF_LINE_SPACING / 10.0f。
	 * 在 applyScalingFromXml() 中赋值。
	 */
	public float SYMBOL_SCALE = 1.0f;

	/** 重新根据 XML tenths / screen 应用所有缩放（MxlDefaults 初始化完成后调用） */
	public void applyScalingFromXml(float tenthsMm, float tenthsPerMmUnit) {
		// step 1: DPI 探测：screenHeight/screenWidth 若可用 → 以 6 寸屏幕 1080x1920 为基准估计 mmToPx
		if (screenWidth > 0 && screenHeight > 0) {
			// 用经验公式：1mm 约等于屏幕短边像素 / 70（Ave Maria 4 systems 适配）
			float shortEdge = Math.min(screenWidth, screenHeight);
			// 6.35mm=40tenths, 1mm≈6.299tenths，最终 STAFF_LINE_SPACING(=10tenths) 约等于 8~16px 最舒服
			// 直接用 tenths:px = 1 : 1.6 作为基准（10 tenths=16px 好显示）
			this.mmToPx = shortEdge / 70f / 25.4f * 25.4f / 10f; // unused，下面直接给 tenthsToPx
			// 目标：1 staff-space (10 tenths) ≈ shortEdge*0.008  ~= 1080*0.008=8.6px  小屏偏细
			// 更稳：固定 1 tenths = 1.5 px → 1 staff-space = 15 px；大乐谱/小屏按 shortEdge 适配：
			float targetPxPerStaffSpace = Math.max(9, Math.min(20, shortEdge / 70f));  // 9~20px
			// 10 tenths = 1 staff-space
			this.tenthsToPx = targetPxPerStaffSpace / 10f;
		} else {
			this.tenthsToPx = 1.5f; // 1.5 px per tenth (兜底)
		}

		// XML defaults 若声明 <millimeters>6.35</millimeters><tenths>40</tenths> 就尊重单位：
		// 6.35 mm/40 tenths * mmToPx（不强制，仅作为附加缩放的可选项，我们用屏幕像素优先）
		// step 2: STAFF_LINE_SPACING = 10 tenths × tenthsToPx
		float sp = 10f * this.tenthsToPx;
		this.STAFF_LINE_SPACING = Math.max(5, Math.round(sp));
		// step 3: 派生其他常量
		this.NOTE_LINE_HIGHT = Math.round(this.STAFF_LINE_SPACING * 3.5f);  // 标准 3.5 space
		this.SPACE = Math.max(2, Math.round(this.STAFF_LINE_SPACING * 0.5f));
		this.NOTE_WIDTH = Math.round(this.STAFF_LINE_SPACING * 1.4f);
		this.SYMBOL_SCALE = this.STAFF_LINE_SPACING / 10f;
		DebugLog.i(TAG, "applyScalingFromXml: tenthsToPx=" + this.tenthsToPx
				+ " STAFF_LINE_SPACING=" + this.STAFF_LINE_SPACING
				+ " NOTE_LINE_HIGHT=" + this.NOTE_LINE_HIGHT
				+ " SPACE=" + this.SPACE + " NOTE_WIDTH=" + this.NOTE_WIDTH
				+ " SYMBOL_SCALE=" + this.SYMBOL_SCALE
				+ " screen=" + screenWidth + "x" + screenHeight);
	}

	//每一部分的所有的PaintTransfer
	public Map<String, PaintTransfer> oldPaintTransfer = new HashMap<String, PaintTransfer>();
	public String oldPartID;
	
	public Context context;
	public Bitmap bitmap;
	public SharedPreferences appPrefs;

	public boolean isUpright;
	public int screenWidth;
	public int screenHeight;

	public float zoomX;
	public float zoomY;

	public Canvas canvas;
	public Paint paint;
	
	public MxlDefaults defaults = new MxlDefaults(null, null, null, null);
	public Map<String, MxlScorePart> scoreParts = new HashMap<String, MxlScorePart>();
	//储存所有音节,
	public Map<String, Vector<Note>> scorePartsNotes = new HashMap<String, Vector<Note>>(); 
	
	// -----
	public float systemLeftMargin = 0;
	public float systemRightMargin = 0;
	public float systemTopDistance = 0;
	public float systemDistance = 0;
	// ------
	public float pageWidth;
	public float pageHeight;
	public PVector<MxlPageMargins> pagemargins;
	public SymbolPool symbolPool;	
	
	public int disPageNo = 1;
	public int maxPage = 0;

	/**
	 * 全量收集开关：
	 *  - true：MxlNote/MxlMeasure 跳过"非当前页 return"，把所有页的音符都写入 scorePartsNotes
	 *          同时 PaintTransfer 不真正绘制到 Canvas（不调 drawBitmap/drawLine/drawText/drawPath）
	 *  - false：正常渲染当前页，非当前页直接 return
	 *  解决问题：原先 paint 只渲染 disPageNo 对应页，导致 scorePartsNotes 里只有当前页音符
	 *            → 后面页播放时 plan 为空，表现为"进度在走但没声音"。
	 */
	public boolean collectAllNotesForPlayback = false;

	public void setScreen(int w, int h) {
		this.screenHeight = h;
		this.screenWidth = w;
		if (this.screenHeight < this.screenWidth) {
			isUpright = false;
		} else {
			isUpright = true;
		}
		DebugLog.i(TAG, "setScreen w=" + w + " h=" + h + " isUpright=" + isUpright);
	}

	public void setPage(float w, float h) {
		DebugLog.i(TAG, "setPage pageWidth=" + w + " pageHeight=" + h + " 当前bitmap=" + bitmap);
		this.pageHeight = h;
		this.pageWidth = w;
		if (bitmap == null) {
			int wpx = Math.max(1, Math.round(pageWidth));
			int hpx = Math.max(1, Math.round(pageHeight));
			DebugLog.i(TAG, "Bitmap 尚未创建 → createBitmap " + wpx + "x" + hpx + " RGB_565（需要 " + (wpx*hpx*2/1024) + " KB）");
			long t0 = System.currentTimeMillis();
			bitmap = Bitmap.createBitmap(wpx, hpx, Config.RGB_565);
			long t1 = System.currentTimeMillis();
			DebugLog.i(TAG, "createBitmap 完成 耗时=" + (t1 - t0) + "ms  bitmap.rowBytes="
					+ (bitmap == null ? "null" : bitmap.getRowBytes())
					+ "  byteCount=" + (bitmap == null ? "null" : bitmap.getByteCount()));
		} else {
			DebugLog.d(TAG, "bitmap 已存在，复用  width=" + bitmap.getWidth() + " height=" + bitmap.getHeight());
		}
		long t0 = System.currentTimeMillis();
		canvas = new Canvas(bitmap);
		String bgColor = "white";
		try { bgColor = appPrefs == null ? "white" : appPrefs.getString("background", "white"); } catch (Throwable ignore) {}
		try {
			canvas.drawColor(Color.parseColor(bgColor));
			DebugLog.d(TAG, "Canvas 初始化，刷背景色=" + bgColor + "  canvas.isHardwareAccelerated=" + canvas.isHardwareAccelerated());
		} catch (Throwable t) {
			DebugLog.e(TAG, "canvas.drawColor(background) 失败 bg=" + bgColor, t);
			throw t;
		}
		long t1 = System.currentTimeMillis();
		DebugLog.i(TAG, "setPage 收尾 new Canvas + drawColor 耗时=" + (t1 - t0) + "ms");
	}

	public void setAutoZoom() {
		DebugLog.d(TAG, "setAutoZoom: page=" + pageWidth + "x" + pageHeight
				+ " screen=" + screenWidth + "x" + screenHeight);
		if (this.pageWidth > 0 && this.screenWidth > 0) {
			this.zoomX = screenWidth / pageWidth;
		}
		if (this.pageHeight > 0 && this.screenHeight > 0) {
			this.zoomY = screenHeight / pageHeight;
		}
		// 等比例
		zoomY = zoomX > zoomY ? zoomX : zoomY;
		zoomX = zoomY;
		DebugLog.i(TAG, "setAutoZoom 最终 zoomX=" + zoomX + " zoomY=" + zoomY
				+ " (canvas.scale 之后，乐谱放大比例)");
		canvas.scale(zoomX, zoomY);
	}

	public int getZoomedX(float x) {
		return Math.round(x * this.zoomX);
	}

	public int getZoomedY(float y) {
		return Math.round(y * this.zoomY);
	}


	public void setDisPageNo(int disPageNo) {
		DebugLog.d(TAG, "setDisPageNo " + disPageNo + "（当前已知 maxPage=" + maxPage + "）");
		this.disPageNo = disPageNo;
	}

	public int getDisPageNo() {
		return disPageNo;
	}
	
}
