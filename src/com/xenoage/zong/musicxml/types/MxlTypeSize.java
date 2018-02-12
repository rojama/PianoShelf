package com.xenoage.zong.musicxml.types;

import org.w3c.dom.Element;

import com.xenoage.util.xml.XMLReader;
import com.xenoage.zong.musicxml.util.Parse;

public enum MxlTypeSize {
	FULL, CUE, LARGE;
	
	public static MxlTypeSize read(Element e) {
		String s = XMLReader.attribute(e, "size");
	    return Parse.getEnumValue(s, values());
	}
}
