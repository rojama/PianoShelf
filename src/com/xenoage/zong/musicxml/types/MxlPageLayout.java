package com.xenoage.zong.musicxml.types;

import com.rojama.pianoshelf.PaintTransfer;
import com.xenoage.pdlib.PVector;
import com.xenoage.util.annotations.MaybeEmpty;
import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

@IncompleteMusicXML(partly = "")
public final class MxlPageLayout {
	public static final String ELEM_NAME = "page-layout";

	@MaybeNull
	private final Float pageHeight;

	@MaybeNull
	private final Float pageWidth;

	@MaybeEmpty
	private final PVector<MxlPageMargins> pageMargins;

	public MxlPageLayout(Float pageHeight, Float pageWidth, PVector<MxlPageMargins> pageMargins) {
		this.pageHeight = pageHeight;
		this.pageWidth = pageWidth;
		this.pageMargins = pageMargins;
	}

	@MaybeNull
	public Float getPageHeight() {
		return this.pageHeight;
	}

	@MaybeNull
	public Float getPageWidth() {
		return this.pageWidth;
	}

	@MaybeEmpty
	public PVector<MxlPageMargins> getPageMargins() {
		return this.pageMargins;
	}

	@NeverNull
	public static MxlPageLayout read(Element e) {
		Float pageHeight = null;
		Float pageWidth = null;
		PVector pageMargins = PVector.pvec();
		for (Element c : XMLReader.elements(e)) {
			String n = c.getNodeName();
			switch (n.charAt(5)) {
			case 'h':
				if (pageHeight != null)
					break;
				pageHeight = Float.valueOf(Parse.parseFloat(c));
				break;
			case 'm':
				pageMargins = pageMargins.plus(MxlPageMargins.read(c));
				break;
			case 'w':
				if (pageWidth != null)
					break;
				pageWidth = Float.valueOf(Parse.parseFloat(c));
			}
		}

		return new MxlPageLayout(pageHeight, pageWidth, pageMargins);
	}

	public void write(Element parent) {
		Element e = XMLWriter.addElement("page-layout", parent);
		if (this.pageHeight != null) {
			XMLWriter.addElement("page-height", this.pageHeight, e);
			XMLWriter.addElement("page-width", this.pageWidth, e);
		}
		for (MxlPageMargins item : this.pageMargins)
			item.write(e);
	}

	public void paint(PaintTransfer pt) {
		Float pageHeight = this.getPageHeight()!=null?this.getPageHeight():pt.getPageHeight();
		Float pageWidth = this.getPageWidth()!=null?this.getPageWidth():pt.getPageWidth();
		pt.ct.setPage(pageWidth, pageHeight);
		pt.ct.pagemargins = this.getPageMargins(); //TODO ∏≤∏«¥¶¿Ì
	}
}