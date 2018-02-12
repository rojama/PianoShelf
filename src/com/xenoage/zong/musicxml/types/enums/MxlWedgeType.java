package com.xenoage.zong.musicxml.types.enums;

import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public enum MxlWedgeType
{
  Crescendo, 
  Diminuendo, 
  Stop;

  public static MxlWedgeType read(String s, Element parent)
  {
    return InvalidMusicXML.throwNull(Parse.getEnumValue(s, values()), parent);
  }

  public String write()
  {
    return toString().toLowerCase();
  }
}