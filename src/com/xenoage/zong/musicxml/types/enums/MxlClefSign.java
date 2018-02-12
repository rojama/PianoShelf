package com.xenoage.zong.musicxml.types.enums;

import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public enum MxlClefSign
  implements EnumWithXMLNames
{
  G("G"), 
  F("F"), 
  C("C"), 
  Percussion("percussion"), 
  TAB("TAB"), 
  None("none");

  public static final String ELEM_NAME = "sign";
  private final String xmlName;

  private MxlClefSign(String xmlName)
  {
    this.xmlName = xmlName;
  }

  @Override
public String getXMLName()
  {
    return this.xmlName;
  }

  @NeverNull
  public static MxlClefSign read(Element e) {
    return InvalidMusicXML.throwNull(Parse.getEnumValueNamed(XMLReader.text(e), values()), e);
  }

  public void write(Element parent)
  {
    XMLWriter.addElement("sign", this.xmlName, parent);
  }
}