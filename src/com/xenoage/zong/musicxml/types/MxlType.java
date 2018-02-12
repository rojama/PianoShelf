package com.xenoage.zong.musicxml.types;

import org.w3c.dom.Element;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import com.xenoage.zong.musicxml.util.Parse;

public class MxlType {

	public MxlType(MxlTypeValue type, MxlTypeSize size) {
		super();
		this.type = type;
		this.size = size;
	}

	public static final String ELEM_NAME = "type";

	@NeverNull
	public final MxlTypeValue type;

	@MaybeNull
	public final MxlTypeSize size;

	public static enum MxlTypeValue {
		_256TH, _128TH, _64TH, _32ND, _16TH, EIGHTH, QUARTER, HALF, WHOLE, BREVE, LONG;
	}

	public static MxlType read(Element e) {
		String s = XMLReader.text(e);
		return new MxlType(InvalidMusicXML.throwNull(Parse.getEnumValue(s, MxlTypeValue.values()),
				e), MxlTypeSize.read(e));
	}

}
