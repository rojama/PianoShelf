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

import com.xenoage.pdlib.PVector;
import com.xenoage.zong.musicxml.types.MxlDefaults;
import com.xenoage.zong.musicxml.types.MxlNote;
import com.xenoage.zong.musicxml.types.MxlPageMargins;
import com.xenoage.zong.musicxml.types.MxlScorePart;
import com.xenoage.zong.symbols.SymbolPool;

public class CommonTransfer {
	private static final String TAG = "CommonTransfer";

	public final int PART_NAME_SIZE = 15;
	public final int SPACE = 5;
	public final int NOTE_LINE_HIGHT = 34;
	public final int NOTE_WIDTH = 15; // 音符宽度用于兼容判断紧挨的两个音符是否是同一个位置

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
