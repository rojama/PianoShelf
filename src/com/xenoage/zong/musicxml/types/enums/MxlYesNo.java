package com.xenoage.zong.musicxml.types.enums;

import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import org.w3c.dom.Element;

public class MxlYesNo
{
  public static boolean read(String s, Element e)
  {
    if (s.equals("yes"))
      return true;
    if (s.equals("no"))
      return false;
    throw InvalidMusicXML.invalid(e);
  }

  public static Boolean readNull(String s, Element e)
  {
    if (s == null) {
      return null;
    }
    return Boolean.valueOf(read(s, e));
  }

  public static String write(boolean v)
  {
    return v ? "yes" : "no";
  }
}