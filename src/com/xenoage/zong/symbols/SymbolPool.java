package com.xenoage.zong.symbols;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.Hashtable;

import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;

import com.rojama.pianoshelf.R;
import com.xenoage.util.error.Err;
import com.xenoage.util.error.ErrorLevel;
import com.xenoage.zong.symbols.common.CommonSymbol;
import com.xenoage.zong.symbols.common.CommonSymbolPool;
import com.xenoage.zong.symbols.loader.SVGSymbolLoader;

public class SymbolPool {
	private String id;
	private Hashtable<String, Symbol> symbols;
	private CommonSymbolPool commonSymbolPool;
	private WarningSymbol warningSymbol = new WarningSymbol();
	private Date latestSymbolDate;

	public static SymbolPool empty() {
		return new SymbolPool();
	}

	public static SymbolPool load(String id, Context context) {
		try {
			return new SymbolPool(id, context);
		} catch (Exception ex) {
		}

		return null;
	}

	public static SymbolPool loadDefault(Context context) {
		return load("default", context);
	}

	private SymbolPool() {
		this.id = null;
		this.symbols = new Hashtable<String, Symbol>(0);
		this.commonSymbolPool = new CommonSymbolPool();
	}

	public SymbolPool(String id, Context context) throws FileNotFoundException {
		this.id = id;
		this.symbols = new Hashtable<String, Symbol>(0);
		this.commonSymbolPool = new CommonSymbolPool();

		Resources res = context.getResources();
		int xmlIndex = R.xml.tex_default;
		int pngIndex = R.drawable.tex_default;
		if (id.equals("default")) {
			xmlIndex = R.xml.tex_default;
			pngIndex = R.drawable.tex_default;
		}
		// TODO 增加样式

		try {
			Bitmap warningBitmap = Bitmap.createBitmap(10, 40, Config.ARGB_8888);
			for (int i=0;i<10;i++)
				warningBitmap.setPixel(i, 20, Color.RED);
			for (int i=0;i<40;i++)
				warningBitmap.setPixel(5, i, Color.RED);
			this.warningSymbol.setBitmap(warningBitmap);
			
			
			BitmapDrawable bmpDraw = (BitmapDrawable) res.getDrawable(pngIndex);
			Bitmap bitmap = bmpDraw.getBitmap();
			int bitmapWidth = bitmap.getWidth();
			int bitmapHeight = bitmap.getHeight();
			
			XmlResourceParser xpp = res.getXml(xmlIndex);
			SVGSymbolLoader loader = new SVGSymbolLoader();
			xpp.next();
			int eventType = xpp.getEventType();
			
			/**
			//生成总图用
			int x=50,y=50,maxh =0;
			Bitmap bm = Bitmap.createBitmap(800 , 2500 , Config.ARGB_8888);  
	        Canvas canvas_symbol = new  Canvas(bm);  
			**/
			
			while (eventType != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_TAG) {
					if (xpp.getName().equals("texture")) {
						RectSymbol symbol = loader.loadSymbol(xpp.getAttributeValue(0), Float
								.parseFloat(xpp.getAttributeValue(1)), Float.parseFloat(xpp
								.getAttributeValue(2)), Float.parseFloat(xpp.getAttributeValue(3)),
								Float.parseFloat(xpp.getAttributeValue(4)));
						RectF rectf = symbol.getBoundingRect();
						Rect rect = new Rect();
						rect.left = Math.round(rectf.left * bitmapWidth);
						rect.right = Math.round(rectf.right * bitmapWidth);
						rect.top = Math.round(rectf.top * bitmapHeight);
						rect.bottom = Math.round(rectf.bottom * bitmapHeight);
						int w = rect.width();
						int h = rect.height();
						//System.out.println(symbol.getID() + "__" + w + ":" + h);

						Matrix m = new Matrix();
						if (bitmapWidth == 512) {
							m.postScale(0.5F, 0.5F);
						}
						Bitmap bitmapSymbol = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect
								.width(), rect.height(), m, true);
						symbol.setTopToBase(Math.round(bitmapSymbol.getHeight()
								* Float.parseFloat(xpp.getAttributeValue(5))));
						symbol.setBitmap(bitmapSymbol);						
						this.symbols.put(symbol.getID(), symbol);
						
						
						/**
						//生成总图用
						Paint paint = new Paint();
						paint.setColor(Color.BLACK);						
						if (x+bitmapSymbol.getWidth()+110 > 750) {
							x = 50;
							y += maxh+80;
							maxh =0;
							
						}												
						canvas_symbol.drawBitmap(bitmapSymbol, x, y, paint);
						paint.setTextSize(10);
						canvas_symbol.drawText(xpp.getAttributeValue(0), x, y+40+bitmapSymbol.getHeight(), paint);
						
						if (bitmapSymbol.getHeight()>maxh){
							maxh = bitmapSymbol.getHeight();
						}
						x += bitmapSymbol.getWidth()+110;
						**/
					}
				}
				eventType = xpp.next();
			}// eof-while
			
			/**
			//生成总图用
			canvas_symbol.save(Canvas.ALL_SAVE_FLAG );  
	        canvas_symbol.restore();  
	          
	        File f = new  File( "/sdcard/all_symbol.png" );  
	        FileOutputStream fos = null ;  
	        try  {  
	            fos = new  FileOutputStream(f);  
	            bm.compress(Bitmap.CompressFormat.PNG, 100 , fos);  
	        } catch  (FileNotFoundException e) {  
	            // TODO Auto-generated catch block   
	            e.printStackTrace();  
	        } 
	        **/
	        
		} catch (Exception ex) {
			ex.printStackTrace();
			Err.err().report(ErrorLevel.Fatal, "Error_CouldNotLoadSymbolPool", ex);
		}

		this.commonSymbolPool = new CommonSymbolPool();
		this.commonSymbolPool.update(this);		
	}

	public Symbol getSymbol(String id) {
		return this.symbols.get(id);
	}

	public Symbol getSymbol(CommonSymbol commonSymbol) {
		Symbol ret = this.commonSymbolPool.getSymbol(commonSymbol);
		if (ret == null)
			ret = this.warningSymbol;
		return ret;
	}

	public String getID() {
		return this.id;
	}

	public Hashtable<String, Symbol> getSymbols() {
		return this.symbols;
	}

	public Date getLatestSymbolDate() {
		return this.latestSymbolDate;
	}

	public float computeNumberWidth(int number, float gap) {
		float ret = 0.0F;
		String s = Integer.toString(number);
		for (int i = 0; i < s.length(); i++) {
			char d = s.charAt(i);
			Symbol symbol = getSymbol("digit-" + d);
			if (symbol == null)
				continue;
			ret += symbol.getBoundingRect().width();
			if (i < s.length() - 1) {
				ret += gap;
			}
		}
		return ret;
	}
}