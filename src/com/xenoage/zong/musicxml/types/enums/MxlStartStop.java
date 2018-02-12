package com.xenoage.zong.musicxml.types.enums;

import com.xenoage.util.annotations.NeverNull;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public enum MxlStartStop
{
  Start, 
  Stop;

  @NeverNull
  public static MxlStartStop read(String s, Element parent) {
    return InvalidMusicXML.throwNull(Parse.getEnumValue(s, values()), parent);
  }

  public String write()
  {
    return toString().toLowerCase();
  }
}