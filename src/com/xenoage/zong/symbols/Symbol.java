package com.xenoage.zong.symbols;

import android.graphics.Bitmap;
import android.graphics.RectF;

public abstract interface Symbol {
	public static final float DEFAULT_BASELINE = 0.0F;
	public static final float DEFAULT_ASCENT = 2.0F;

	public abstract String getID();

	public abstract RectF getBoundingRect();

	public abstract void setTexture(SymbolTexture paramSymbolTexture);

	public abstract SymbolTexture getTexture();

	public abstract float getBaselineOffset();

	public abstract float getAscentHeight();

	public abstract float getLeftBorder();

	public abstract float getRightBorder();

	public abstract SymbolType getType();

	public abstract Bitmap getBitmap();

	public abstract int getTopToBase();
}