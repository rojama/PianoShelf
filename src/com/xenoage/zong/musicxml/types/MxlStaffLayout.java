package com.xenoage.zong.musicxml.types;

import com.rojama.pianoshelf.PaintTransfer;
import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public final class MxlStaffLayout {
	public static final String ELEM_NAME = "staff-layout";

	@MaybeNull
	private final Float staffDistance;

	@MaybeNull
	private final Integer number;
	private static final int defaultNumber = 1;

	public MxlStaffLayout(Float staffDistance, Integer number) {
		this.staffDistance = staffDistance;
		this.number = number;
	}

	@MaybeNull
	public Float getStaffDistance() {
		return this.staffDistance;
	}

	@MaybeNull
	public Integer getNumber() {
		return this.number;
	}

	@NeverNull
	public int getNumberNotNull() {
		return this.number != null ? this.number.intValue() : 1;
	}

	@NeverNull
	public static MxlStaffLayout read(Element e) {
		return new MxlStaffLayout(Parse.parseChildFloatNull(e, "staff-distance"), Parse
				.parseAttrIntNull(e, "number"));
	}

	public void write(Element parent) {
		Element e = XMLWriter.addElement("staff-layout", parent);
		if (this.staffDistance != null)
			XMLWriter.addElement("staff-distance", this.staffDistance, e);
		XMLWriter.addAttribute(e, "number", this.number);
	}

	public void paint(PaintTransfer pt) {
		pt.staffLayout.put(this.getNumber(), this.getStaffDistance());
	}
}