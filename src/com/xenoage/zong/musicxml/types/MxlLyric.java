package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverEmpty;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.attributes.MxlPosition;
import com.xenoage.zong.musicxml.types.choice.MxlLyricContent;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing = "end-line,end-paragraph,editorial,number,name,justify,position,placement,color", children = "", partly = "")
public final class MxlLyric {
	public static final String ELEM_NAME = "lyric";

	@NeverEmpty
	private final MxlLyricContent content;

	@NeverNull
	private final MxlPosition position;

	public MxlLyric(MxlLyricContent content, MxlPosition position) {
		this.content = content;
		this.position = position;
	}

	@NeverNull
	public MxlPosition getPosition() {
		return this.position;
	}

	public MxlLyricContent getContent() {
		return this.content;
	}

	@MaybeNull
	public static MxlLyric read(Element e) {
		MxlLyricContent content = null;
		Element firstChild = XMLReader.element(e);
		String n = firstChild.getNodeName();
		if ((n.equals("syllabic")) || (n.equals("text"))) {
			content = MxlSyllabicText.read(e);
		} else if (n.equals("extend")) {
			content = MxlExtend.read(e);
		}
		if (content != null) {
			return new MxlLyric(content, MxlPosition.read(e));
		}

		return null;
	}

	public void write(Element parent) {
		Element e = XMLWriter.addElement("lyric", parent);
		this.content.write(e);
	}
}