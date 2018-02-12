package com.xenoage.zong.symbols;

import com.xenoage.util.math.TextureRectangle2f;

public class SymbolTexture {
	private int textureID;
	private TextureRectangle2f texCords;

	public SymbolTexture(int textureID, TextureRectangle2f texCords) {
		this.textureID = textureID;
		this.texCords = texCords;
	}

	public int getTextureID() {
		return this.textureID;
	}

	public TextureRectangle2f getTexCords() {
		return this.texCords;
	}
}