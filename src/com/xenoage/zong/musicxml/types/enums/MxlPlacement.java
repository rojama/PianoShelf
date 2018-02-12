package com.xenoage.zong.musicxml.types.enums;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public enum MxlPlacement
{
  Above, 
  Below;

  public static final String ATTR_NAME = "placement";

  @MaybeNull
  public static MxlPlacement read(Element e) {
    String s = XMLReader.attribute(e, "placement");
    return Parse.getEnumValue(s, values());
  }

  public void write(Element e)
  {
    XMLWriter.addAttribute(e, "placement", toString().toLowerCase());
  }
}