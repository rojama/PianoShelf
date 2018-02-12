package com.xenoage.zong.musicxml.types;

import com.xenoage.util.NullTools;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.enums.MxlBeamValue;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing = "repeater,fan,color", children = "", partly = "")
public final class MxlBeam {
	public static final String ELEM_NAME = "beam";

	@NeverNull
	private final MxlBeamValue value;
	private final int number;
	private static final int defaultNumber = 1;

	public MxlBeam(MxlBeamValue value, int number) {
		this.value = value;
		this.number = number;
	}

	@NeverNull
	public MxlBeamValue getValue() {
		return this.value;
	}

	public int getNumber() {
		return this.number;
	}

	@NeverNull
	public static MxlBeam read(Element e) {
		return new MxlBeam(MxlBeamValue.read(e), (NullTools.notNull(Parse.parseAttrIntNull(e,
				"number"), Integer.valueOf(1))).intValue());
	}

	public void write(Element parent) {

		Element e = XMLWriter.addElement("beam", parent);
		this.value.write(e);
		XMLWriter.addAttribute(e, "number", Integer.valueOf(this.number));
	}
	
	public int hashCode(){
		return number;		
	}
}