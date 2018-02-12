package com.xenoage.zong.musicxml.types.enums;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public enum MxlMarginType
  implements EnumWithXMLNames
{
  Odd("odd"), 
  Even("even"), 
  Both("both");

  public static final String ATTR_NAME = "type";
  private final String xmlName;

  private MxlMarginType(String xmlName)
  {
    this.xmlName = xmlName;
  }

  @Override
public String getXMLName()
  {
    return this.xmlName;
  }

  @MaybeNull
  public static MxlMarginType read(Element e) {
    return Parse.getEnumValueNamed(XMLReader.attribute(e, "type"), values());
  }

  public void write(Element e)
  {
    XMLWriter.addAttribute(e, "type", this.xmlName);
  }
}