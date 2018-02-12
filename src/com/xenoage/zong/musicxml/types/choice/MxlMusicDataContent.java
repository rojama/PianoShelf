package com.xenoage.zong.musicxml.types.choice;

import org.w3c.dom.Element;

import com.rojama.pianoshelf.PaintTransfer;

public abstract interface MxlMusicDataContent {
	public abstract MxlMusicDataContentType getMusicDataContentType();

	public abstract void write(Element paramElement);

	public static enum MxlMusicDataContentType {
		Note, Backup, Forward, Direction, Attributes, Print, Barline;
	}

	public abstract void print(PaintTransfer pt);
}