package com.xenoage.zong.musicxml.types.choice;

import com.rojama.pianoshelf.PaintTransfer;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.zong.musicxml.types.MxlFullNote;
import org.w3c.dom.Element;

public abstract interface MxlNoteContent {
	public abstract MxlNoteContentType getNoteContentType();

	@NeverNull
	public abstract MxlFullNote getFullNote();

	public abstract void write(Element paramElement);

	public static enum MxlNoteContentType {
		Grace, Cue, Normal;
	}
}