package com.xenoage.zong.musicxml.types;

import com.rojama.pianoshelf.PaintTransfer;
import com.xenoage.util.NullTools;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.core.music.Pitch;
import com.xenoage.zong.musicxml.types.choice.MxlFullNoteContent;
import com.xenoage.zong.musicxml.util.InvalidCore;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import com.xenoage.zong.symbols.RectSymbol;

import org.w3c.dom.Element;

public final class MxlPitch implements MxlFullNoteContent {
	public static final String XML_NAME = "pitch";

	@NeverNull
	private final Pitch pitch;

	public MxlPitch(Pitch pitch) {
		this.pitch = pitch;
	}

	@NeverNull
	public Pitch getPitch() {
		return this.pitch;
	}

	@Override
	public MxlFullNoteContent.MxlFullNoteContentType getFullNoteContentType() {
		return MxlFullNoteContent.MxlFullNoteContentType.Pitch;
	}

	@NeverNull
	public static MxlPitch read(Element e) {
		return new MxlPitch(Pitch.pi(readStep(e), (NullTools.notNull(Parse.parseChildIntNull(e,
				"alter"), Integer.valueOf(0))).intValue(), Parse.parseChildInt(e, "octave")));
	}

	@Override
	public void write(Element parent) {
		Element e = XMLWriter.addElement("pitch", parent);
		XMLWriter.addElement("step", Character.valueOf(writeStep(this.pitch.getStep())), e);
		XMLWriter.addElement("alter", Integer.valueOf(this.pitch.getAlter()), e);
		XMLWriter.addElement("octave", Integer.valueOf(this.pitch.getOctave()), e);
	}

	private static int readStep(Element e) {
		switch (XMLReader.elementText(e, "step").charAt(0)) {
		case 'C':
			return 0;
		case 'D':
			return 1;
		case 'E':
			return 2;
		case 'F':
			return 3;
		case 'G':
			return 4;
		case 'A':
			return 5;
		case 'B':
			return 6;
		}
		throw InvalidMusicXML.invalid(e);
	}

	private static char writeStep(int step) {
		switch (step) {
		case 0:
			return 'C';
		case 1:
			return 'D';
		case 2:
			return 'E';
		case 3:
			return 'F';
		case 4:
			return 'G';
		case 5:
			return 'A';
		case 6:
			return 'B';
		}
		throw InvalidCore.invalidCore(Integer.valueOf(step));
	}
}