package com.xenoage.zong.musicxml.types.enums;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public enum MxlVAlign
{
  Top, 
  Middle, 
  Bottom, 
  Baseline;

  public static final String ATTR_NAME = "valign";

  @MaybeNull
  public static MxlVAlign read(Element e) {
    String s = XMLReader.attribute(e, "valign");
    if (s != null)
    {
      return InvalidMusicXML.throwNull(Parse.getEnumValue(s, values()), e);
    }

    return null;
  }

  public void write(Element e)
  {
    e.setAttribute("valign", toString().toLowerCase());
  }
}