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

import com.rojama.pianoshelf.DebugLog;
import com.rojama.pianoshelf.R;
import com.xenoage.util.error.Err;
import com.xenoage.util.error.ErrorLevel;
import com.xenoage.zong.symbols.common.CommonSymbol;
import com.xenoage.zong.symbols.common.CommonSymbolPool;
import com.xenoage.zong.symbols.loader.SVGSymbolLoader;

public class SymbolPool {
	private static final String TAG = "SymbolPool";
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
		} catch (Throwable ex) {
			// 捕获所有 Throwable（包括 Error），防止资源加载失败时返回 null 而不留下任何线索
			DebugLog.e(TAG, "SymbolPool.load(\"" + id + "\") 加载失败", ex);
			ex.printStackTrace();
		}

		return null;
	}

	public static SymbolPool loadDefault(Context context) {
		DebugLog.i(TAG, "loadDefault(id=default) 开始…");
		long t0 = System.currentTimeMillis();
		SymbolPool sp = load("default", context);
		long t1 = System.currentTimeMillis();
		if (sp == null) {
			DebugLog.e(TAG, "loadDefault 返回 null（这是致命错误，乐谱符号会全空）");
		} else {
			DebugLog.i(TAG, "loadDefault 完成 耗时=" + (t1 - t0) + "ms  装载 symbols 数量="
					+ (sp.symbols == null ? -1 : sp.symbols.size()));
		}
		return sp;
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
		DebugLog.d(TAG, "SymbolPool 构造 id=" + id + " 开始加载资源");

		Resources res = context.getResources();
		int xmlIndex = R.xml.tex_default;
		int pngIndex = R.drawable.tex_default;
		if (id.equals("default")) {
			xmlIndex = R.xml.tex_default;
			pngIndex = R.drawable.tex_default;
		}
		// TODO 增加样式

		try {
			DebugLog.v(TAG, "  [1/3] 创建 WarningSymbol");
			Bitmap warningBitmap = Bitmap.createBitmap(10, 40, Config.ARGB_8888);
			for (int i=0;i<10;i++)
				warningBitmap.setPixel(i, 20, Color.RED);
			for (int i=0;i<40;i++)
				warningBitmap.setPixel(5, i, Color.RED);
			this.warningSymbol.setBitmap(warningBitmap);
			
			
			DebugLog.v(TAG, "  [2/3] 获取 R.drawable.tex_default 位图 + R.xml.tex_default 解析器");
			DebugLog.d(TAG, "        pngIndex=" + pngIndex
					+ "  type=" + res.getResourceTypeName(pngIndex)
					+ "  name=" + res.getResourceEntryName(pngIndex));
			BitmapDrawable bmpDraw = (BitmapDrawable) res.getDrawable(pngIndex);
			if (bmpDraw == null) {
				throw new FileNotFoundException("R.drawable.tex_default 返回 null，资源不存在或无效");
			}
			Bitmap bitmap = bmpDraw.getBitmap();
			if (bitmap == null) {
				throw new FileNotFoundException("tex_default bitmap 为 null");
			}
			int bitmapWidth = bitmap.getWidth();
			int bitmapHeight = bitmap.getHeight();
			DebugLog.d(TAG, "        tex_default.png 尺寸=" + bitmapWidth + "x" + bitmapHeight
					+ " bitmapScaleFactor=" + (bitmapWidth == 512 ? "0.5f (旧 512 纹素)" : "1.0f"));
			
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

			int loaded = 0;
			int skipped = 0;
			long texStart = System.currentTimeMillis();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_TAG) {
					if (xpp.getName().equals("texture")) {
						try {
							// 按属性名而非位置索引取值：XmlResourceParser 在 XML 含 xmlns:android 命名空间时，
							// 会把命名空间声明也暴露为 attribute，导致索引偏移 1 位（index0=namespace, index1=id,…）
							// 这就是 NumberFormatException: "accordion-1-1-1" 被 parseFloat 的根因。
							String symId = xpp.getAttributeValue(null, "id");
							String x1s  = xpp.getAttributeValue(null, "x1");
							String x2s  = xpp.getAttributeValue(null, "x2");
							String y1s  = xpp.getAttributeValue(null, "y1");
							String y2s  = xpp.getAttributeValue(null, "y2");
							String bases = xpp.getAttributeValue(null, "base");
							if (symId == null || x1s == null || x2s == null || y1s == null || y2s == null || bases == null) {
								throw new IllegalArgumentException("texture 属性缺失: id=" + symId + " x1=" + x1s + " x2=" + x2s + " y1=" + y1s + " y2=" + y2s + " base=" + bases);
							}
							RectSymbol symbol = loader.loadSymbol(symId,
									Float.parseFloat(x1s), Float.parseFloat(x2s),
									Float.parseFloat(y1s), Float.parseFloat(y2s));
							RectF rectf = symbol.getBoundingRect();
							Rect rect = new Rect();
							rect.left = Math.round(rectf.left * bitmapWidth);
							rect.right = Math.round(rectf.right * bitmapWidth);
							rect.top = Math.round(rectf.top * bitmapHeight);
							rect.bottom = Math.round(rectf.bottom * bitmapHeight);
							int w = rect.width();
							int h = rect.height();

							Matrix m = new Matrix();
							if (bitmapWidth == 512) {
								m.postScale(0.5F, 0.5F);
							}
							Bitmap bitmapSymbol = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect
									.width(), rect.height(), m, true);
							symbol.setTopToBase(Math.round(bitmapSymbol.getHeight()
									* Float.parseFloat(bases)));
							symbol.setBitmap(bitmapSymbol);						
							this.symbols.put(symbol.getID(), symbol);
							loaded++;
						} catch (Throwable t) {
							DebugLog.w(TAG, "  [skip] 单个 texture 解析失败（忽略，不影响其它符号）: " + xpp.getAttributeValue(null, "id"), t);
							skipped++;
						}
					} else {
						skipped++;
					}
				}
				eventType = xpp.next();
			}// eof-while
			long texEnd = System.currentTimeMillis();
			DebugLog.i(TAG, "  [3/3] 解析 tex_default.xml texture 元素完成  loaded=" + loaded
					+ "  skipped-tags=" + skipped + "  total耗时=" + (texEnd - texStart) + "ms");
			
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
			DebugLog.e(TAG, "SymbolPool 构造抛出异常，将交给 Err.err 上报", ex);
			ex.printStackTrace();
			Err.err().report(ErrorLevel.Fatal, "Error_CouldNotLoadSymbolPool", ex);
		}

		DebugLog.d(TAG, "commonSymbolPool.update(this) 开始构建 common symbol 映射");
		this.commonSymbolPool = new CommonSymbolPool();
		this.commonSymbolPool.update(this);
		DebugLog.i(TAG, "SymbolPool(\"" + id + "\") 最终 symbols 数=" + symbols.size());
	}

	/**
	 * 返回当前装载的音乐符号总数（≈tex_default.xml中 texture 元素数量）。
	 * 若 hashtable 尚未初始化返回 -1。
	 */
	public int getSymbolCount() {
		return symbols == null ? -1 : symbols.size();
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