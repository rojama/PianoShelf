package com.xenoage.zong.musicxml.types.enums;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.zong.musicxml.util.Parse;

public enum MxlCSSFontSize
  implements EnumWithXMLNames
{
  XXSmall("xx-small"), 
  XSmall("x-small"), 
  Small("small"), 
  Medium("medium"), 
  Large("large"), 
  XLarge("x-large"), 
  XXLarge("xx-large");

  private final String xmlName;

  private MxlCSSFontSize(String xmlName)
  {
    this.xmlName = xmlName;
  }

  @Override
public String getXMLName()
  {
    return this.xmlName;
  }

  @MaybeNull
  public static MxlCSSFontSize read(String s) {
    return Parse.getEnumValueNamed(s, values());
  }
}