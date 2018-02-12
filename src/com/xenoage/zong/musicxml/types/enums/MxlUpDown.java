package com.xenoage.zong.musicxml.types.enums;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public enum MxlUpDown
{
  Up, 
  Down;

  @MaybeNull
  public static MxlUpDown read(String s, Element parent) {
    return Parse.getEnumValue(s, values());
  }

  public String write()
  {
    return toString().toLowerCase();
  }
}