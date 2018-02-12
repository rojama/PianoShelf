package com.xenoage.zong.symbols;

import android.graphics.Bitmap;
import android.graphics.RectF;

public class RectSymbol implements Symbol {
	private String id;
	private RectF rectangle;
	private int topToBase;  //顶部到基线的距离
	public int getTopToBase() {
		return topToBase;
	}

	public void setTopToBase(int topToBase) {
		this.topToBase = topToBase;
	}

	private Bitmap bitmap;

	public Bitmap getBitmap() {
		return bitmap;
	}

	public void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
	}

	public RectSymbol(String id, RectF rectangle) {
		this.id = id;
		this.rectangle = rectangle;
	}

	@Override
	public String getID() {
		return this.id;
	}

	@Override
	public RectF getBoundingRect() {
		return this.rectangle;
	}

	@Override
	public void setTexture(SymbolTexture texture) {
	}

	@Override
	public SymbolTexture getTexture() {
		return null;
	}

	@Override
	public float getBaselineOffset() {
		return 0.0F;
	}

	@Override
	public float getAscentHeight() {
		return 2.0F;
	}

	@Override
	public float getLeftBorder() {
		return this.rectangle.left;
	}

	@Override
	public float getRightBorder() {
		return this.rectangle.right;
	}

	@Override
	public SymbolType getType() {
		return SymbolType.RectSymbol;
	}

	public RectF getRectangle() {
		return this.rectangle;
	}
}