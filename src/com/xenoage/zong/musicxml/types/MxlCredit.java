package com.xenoage.zong.musicxml.types;

import com.rojama.pianoshelf.PaintTransfer;
import com.xenoage.util.NullTools;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.choice.MxlCreditContent;
import com.xenoage.zong.musicxml.types.choice.MxlCreditContent.MxlCreditContentType;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing = "link,bookmark", children = "credit-words")
public final class MxlCredit {
	public static final String ELEM_NAME = "credit";

	@NeverNull
	private final MxlCreditContent content;
	private final int page;
	private static final int defaultPage = 1;

	public MxlCredit(MxlCreditContent content, int page) {
		this.content = content;
		this.page = page;
	}

	@NeverNull
	public MxlCreditContent getContent() {
		return this.content;
	}

	public int getPage() {
		return this.page;
	}

	@NeverNull
	public static MxlCredit read(Element e) {
		MxlCreditContent content = null;
		for (Element c : XMLReader.elements(e)) {
			String n = c.getNodeName();
			if (n.equals("credit-image")) {
				content = MxlImage.read(c);
				break;
			}
			if (n.equals("credit-words")) {
				content = MxlCreditWords.read(e);
				break;
			}
		}
		return new MxlCredit(
				InvalidMusicXML.throwNull(content, e),
				(NullTools.notNull(Parse.parseAttrIntNull(e, "page"), Integer.valueOf(1)))
						.intValue());
	}

	public void write(Element parent) {
		Element e = XMLWriter.addElement("credit", parent);
		this.content.write(e);
		XMLWriter.addAttribute(e, "page", Integer.valueOf(this.page));
	}

	public void paint(PaintTransfer pt) {
		if (this.page != pt.ct.getDisPageNo()) return;
		if (this.getContent().getCreditContentType() == MxlCreditContentType.CreditImage) {
			//TODO
		} else if (this.getContent().getCreditContentType() == MxlCreditContentType.CreditWords) {
			MxlCreditWords mxlCreditWords = (MxlCreditWords) this.getContent();
			mxlCreditWords.paint(pt);
		}
	}
}