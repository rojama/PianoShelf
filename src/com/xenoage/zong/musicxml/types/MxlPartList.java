package com.xenoage.zong.musicxml.types;

import com.rojama.pianoshelf.PaintTransfer;
import com.xenoage.pdlib.PVector;
import com.xenoage.util.annotations.NeverEmpty;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.choice.MxlPartListContent;
import com.xenoage.zong.musicxml.types.choice.MxlPartListContent.PartListContentType;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(children = "part-group,score-part", missing = "", partly = "")
public final class MxlPartList {
	public static final String ELEM_NAME = "part-list";

	@NeverEmpty
	private final PVector<MxlPartListContent> content;

	public MxlPartList(PVector<MxlPartListContent> content) {
		this.content = content;
	}

	@NeverEmpty
	public PVector<MxlPartListContent> getContent() {
		return this.content;
	}

	@NeverNull
	public static MxlPartList read(Element e) {
		PVector content = PVector.pvec();
		boolean scorePartFound = false;
		for (Element c : XMLReader.elements(e)) {
			String n = c.getNodeName();
			if (n.equals("part-group")) {
				content = content.plus(MxlPartGroup.read(c));
			} else if (n.equals("score-part")) {
				content = content.plus(MxlScorePart.read(c));
				scorePartFound = true;
			}
		}
		if (!scorePartFound)
			throw InvalidMusicXML.invalid(e);
		return new MxlPartList(content);
	}

	public void write(Element parent) {
		Element e = XMLWriter.addElement("part-list", parent);
		for (MxlPartListContent item : this.content)
			item.write(e);
	}

	public void paint(PaintTransfer pt) {
		// TODO Auto-generated method stub
		for (MxlPartListContent item : this.content){
			if (item.getPartListContentType() == PartListContentType.ScorePart){
				MxlScorePart scorePart = (MxlScorePart)item;
				pt.ct.scoreParts.put(scorePart.getID(), scorePart);
			}
		}		
	}
}