package com.xenoage.zong.musicxml.types;

import com.rojama.pianoshelf.PaintTransfer;
import com.xenoage.pdlib.PVector;
import com.xenoage.util.annotations.NeverEmpty;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.zong.musicxml.types.choice.MxlMusicDataContent;
import com.xenoage.zong.musicxml.types.choice.MxlMusicDataContent.MxlMusicDataContentType;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing = "harmony,figured-bass,sound,grouping,link,bookmark", children = "note,backup,forward,direction,attributes,print,barline", partly = "")
public final class MxlMusicData {

	@NeverEmpty
	private final PVector<MxlMusicDataContent> content;

	public MxlMusicData(PVector<MxlMusicDataContent> content) {
		this.content = content;
	}

	@NeverEmpty
	public PVector<MxlMusicDataContent> getContent() {
		return this.content;
	}

	@NeverNull
	public static MxlMusicData read(Element e) {
		PVector content = PVector.pvec();
		for (Element c : XMLReader.elements(e)) {
			MxlMusicDataContent item = null;
			String n = c.getNodeName();
			switch (n.charAt(0)) {
			case 'a':
				if (!n.equals("attributes"))
					break;
				item = MxlAttributes.read(c);
				break;
			case 'b':
				if (n.equals("backup")) {
					item = MxlBackup.read(c);
				} else {
					if (!n.equals("barline"))
						break;
					item = MxlBarline.read(c);
				}
				break;
			case 'd':
				if (!n.equals("direction"))
					break;
				item = MxlDirection.read(c);
				break;
			case 'f':
				if (!n.equals("forward"))
					break;
				item = MxlForward.read(c);
				break;
			case 'n':
				if (!n.equals("note"))
					break;
				item = MxlNote.read(c);
				break;
			case 'p':
				if (!n.equals("print"))
					break;
				item = MxlPrint.read(c);
			case 'c':
			case 'e':
			case 'g':
			case 'h':
			case 'i':
			case 'j':
			case 'k':
			case 'l':
			case 'm':
			case 'o':
			}
			if (item != null)
				content = content.plus(item);
		}
		if (content.size() < 1)
			throw InvalidMusicXML.invalid(e);
		return new MxlMusicData(content);
	}

	public void write(Element e) {
		for (MxlMusicDataContent item : this.content) {
			item.write(e);
		}
	}

	public void print(PaintTransfer pt) {
		for (MxlMusicDataContent item : this.content) {
			if (item instanceof MxlPrint || item instanceof MxlAttributes || pt.nowPage == pt.ct.getDisPageNo())
				if (!pt.block){
					item.print(pt);
				}
		}
	}
}