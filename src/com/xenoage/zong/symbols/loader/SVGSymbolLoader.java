package com.xenoage.zong.symbols.loader;

import android.graphics.RectF;

import com.xenoage.util.math.Rectangle2f;
import com.xenoage.zong.symbols.RectSymbol;
import com.xenoage.zong.symbols.Symbol;

public class SVGSymbolLoader {
	public RectSymbol loadSymbol(String id, float x1, float x2, float y1, float y2) {
		RectF rect = new RectF(x1, y1, x2, y2);
		RectSymbol ret = new RectSymbol(id, rect);
		return ret;
	}
}