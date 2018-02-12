package com.xenoage.zong.musicxml.types.enums;

import com.xenoage.util.xml.XMLReader;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public enum MxlTimeSymbol
  implements EnumWithXMLNames
{
  Common("common"), 
  Cut("cut"), 
  SingleNumber("single-number"), 
  Normal("normal");

  public static final String ATTR_NAME = "symbol";
  private final String xmlName;

  private MxlTimeSymbol(String xmlName)
  {
    this.xmlName = xmlName;
  }

  @Override
public String getXMLName()
  {
    return this.xmlName;
  }

  public static MxlTimeSymbol read(Element e)
  {
    String s = XMLReader.attribute(e, "symbol");
    if (s != null)
    {
      MxlTimeSymbol ret = Parse.getEnumValueNamed(s, values());
      return InvalidMusicXML.throwNull(ret, e);
    }

    return null;
  }

  public void write(Element e)
  {
    e.setAttribute("symbol", this.xmlName);
  }
}