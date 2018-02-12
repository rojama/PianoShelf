package com.xenoage.zong.musicxml.types.choice;

import org.w3c.dom.Element;

import com.rojama.pianoshelf.PaintTransfer;

public abstract interface MxlFullNoteContent {
	public abstract MxlFullNoteContentType getFullNoteContentType();

	public abstract void write(Element paramElement);

	public static enum MxlFullNoteContentType {
		Pitch, Unpitched, Rest;
	}

}