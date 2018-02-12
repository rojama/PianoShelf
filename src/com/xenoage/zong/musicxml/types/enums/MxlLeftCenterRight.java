package com.xenoage.zong.musicxml.types.enums;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public enum MxlLeftCenterRight
{
  Left, 
  Center, 
  Right;

  @MaybeNull
  public static MxlLeftCenterRight read(Element e, String attrName) {
    String s = XMLReader.attribute(e, attrName);
    if (s != null)
    {
      return InvalidMusicXML.throwNull(Parse.getEnumValue(s, values()), e);
    }

    return null;
  }

  public void write(Element e, String attrName)
  {
    e.setAttribute(attrName, toString().toLowerCase());
  }
}