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
	}

	public void setPage(float w, float h) {
		this.pageHeight = h;
		this.pageWidth = w;
		if (bitmap == null) {
			bitmap = Bitmap.createBitmap(Math.round(pageWidth), Math.round(pageHeight),
					Config.RGB_565);
		}
		canvas = new Canvas(bitmap);
		canvas.drawColor(Color.parseColor(appPrefs.getString("background", "white")));
		// this.setAutoZoom();
	}

	public void setAutoZoom() {
		if (this.pageWidth > 0 && this.screenWidth > 0) {
			this.zoomX = screenWidth / pageWidth;
		}
		if (this.pageHeight > 0 && this.screenHeight > 0) {
			this.zoomY = screenHeight / pageHeight;
		}
		// 等比例
		zoomY = zoomX > zoomY ? zoomX : zoomY;
		zoomX = zoomY;
		canvas.scale(zoomX, zoomY);
	}

	public int getZoomedX(float x) {
		return Math.round(x * this.zoomX);
	}

	public int getZoomedY(float y) {
		return Math.round(y * this.zoomY);
	}


	public void setDisPageNo(int disPageNo) {
		this.disPageNo = disPageNo;
	}

	public int getDisPageNo() {
		return disPageNo;
	}
	
}
