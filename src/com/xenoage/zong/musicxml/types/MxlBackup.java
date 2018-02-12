package com.xenoage.zong.musicxml.types;

import com.rojama.pianoshelf.PaintTransfer;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.choice.MxlMusicDataContent;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing = "editorial", children = "", partly = "")
public final class MxlBackup implements MxlMusicDataContent {
	public static final String ELEM_NAME = "backup";
	private final int duration;

	public MxlBackup(int duration) {
		this.duration = duration;
	}

	public int getDuration() {
		return this.duration;
	}

	@Override
	public MxlMusicDataContent.MxlMusicDataContentType getMusicDataContentType() {
		return MxlMusicDataContent.MxlMusicDataContentType.Backup;
	}

	@NeverNull
	public static MxlBackup read(Element e) {
		return new MxlBackup(Parse.parseChildInt(e, "duration"));
	}

	@Override
	public void write(Element parent) {
		Element e = XMLWriter.addElement("backup", parent);
		XMLWriter.addElement("duration", Integer.valueOf(this.duration), e);
	}

	@Override
	public void print(PaintTransfer pt) {
		pt.nowDuration -= this.duration;
	}
}