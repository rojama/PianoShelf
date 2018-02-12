package com.xenoage.zong.musicxml.types.enums;

import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public enum MxlBackwardForward
{
  Backward, 
  Forward;

  public static final String ATTR_NAME = "direction";

  @NeverNull
  public static MxlBackwardForward read(Element e) {
    String s = XMLReader.attribute(e, "direction");
    if (s != null)
    {
      return InvalidMusicXML.throwNull(Parse.getEnumValue(s, values()), e);
    }

    throw InvalidMusicXML.invalid(e);
  }

  public void write(Element e)
  {
    e.setAttribute("direction", toString().toLowerCase());
  }
}