package com.xenoage.zong.musicxml.types.enums;

import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public enum MxlGroupBarlineValue
  implements EnumWithXMLNames
{
  Yes("yes"), 
  No("no"), 
  Mensurstrich("Mensurstrich");

  private final String xmlName;

  private MxlGroupBarlineValue(String xmlName)
  {
    this.xmlName = xmlName;
  }

  @Override
public String getXMLName()
  {
    return this.xmlName;
  }

  @NeverNull
  public static MxlGroupBarlineValue read(Element e) {
    String s = XMLReader.text(e);
    if (s != null)
    {
      return InvalidMusicXML.throwNull(Parse.getEnumValueNamed(s, values()), e);
    }

    throw InvalidMusicXML.invalid(e);
  }

  public void write(Element e)
  {
    e.setTextContent(this.xmlName);
  }
}