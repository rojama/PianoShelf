package com.xenoage.zong.symbols;

import android.graphics.Bitmap;
import android.graphics.RectF;

public class WarningSymbol implements Symbol {
	public static final WarningSymbol instance = new WarningSymbol();

	private Bitmap bitmap;

	public Bitmap getBitmap() {
		return bitmap;
	}

	public void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
	}

	
	@Override
	public String getID() {
		return "warning";
	}

	@Override
	public RectF getBoundingRect() {
		return new RectF(0.0F, 0.0F, 0.0F, 0.0F);
	}

	@Override
	public void setTexture(SymbolTexture texture) {
		throw new IllegalStateException("Warning symbol has no symbol texture");
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
		return 0.0F;
	}

	@Override
	public float getRightBorder() {
		return 0.0F;
	}

	@Override
	public SymbolType getType() {
		return SymbolType.WarningSymbol;
	}

	@Override
	public int getTopToBase() {
		return this.getBitmap().getHeight();
	}
}