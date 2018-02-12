package com.xenoage.zong.musicxml.types.enums;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public enum MxlRightLeftMiddle
{
  Right, 
  Left, 
  Middle;

  public static final String ATTR_NAME = "location";

  @MaybeNull
  public static MxlRightLeftMiddle read(Element e) {
    String s = XMLReader.attribute(e, "location");
    if (s != null)
    {
      return InvalidMusicXML.throwNull(Parse.getEnumValue(s, values()), e);
    }

    return null;
  }

  public void write(Element e)
  {
    e.setAttribute("location", toString().toLowerCase());
  }
}