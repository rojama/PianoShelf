package com.xenoage.zong.musicxml.types;

import com.rojama.pianoshelf.PaintTransfer;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.choice.MxlMusicDataContent;
import com.xenoage.zong.musicxml.types.groups.MxlEditorialVoice;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing = "staff", children = "editorial-voice", partly = "")
public final class MxlForward implements MxlMusicDataContent {
	public static final String ELEM_NAME = "forward";
	private final int duration;

	@NeverNull
	private final MxlEditorialVoice editorialVoice;

	public MxlForward(int duration, MxlEditorialVoice editorialVoice) {
		this.duration = duration;
		this.editorialVoice = editorialVoice;
	}

	public int getDuration() {
		return this.duration;
	}

	@NeverNull
	public MxlEditorialVoice getEditorialVoice() {
		return this.editorialVoice;
	}

	@Override
	public MxlMusicDataContent.MxlMusicDataContentType getMusicDataContentType() {
		return MxlMusicDataContent.MxlMusicDataContentType.Forward;
	}

	@NeverNull
	public static MxlForward read(Element e) {
		int duration = Parse.parseChildInt(e, "duration");
		MxlEditorialVoice editorialVoice = MxlEditorialVoice.read(e);
		return new MxlForward(duration, editorialVoice);
	}

	@Override
	public void write(Element parent) {
		Element e = XMLWriter.addElement("forward", parent);
		XMLWriter.addElement("duration", Integer.valueOf(this.duration), e);
		this.editorialVoice.write(e);
	}

	@Override
	public void print(PaintTransfer pt) {
		pt.nowDuration += this.duration;
	}
}