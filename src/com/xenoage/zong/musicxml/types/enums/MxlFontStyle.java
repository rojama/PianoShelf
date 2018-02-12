package com.xenoage.zong.musicxml.types.enums;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public enum MxlFontStyle
{
  Normal, 
  Italic;

  public static final String ATTR_NAME = "font-style";

  @MaybeNull
  public static MxlFontStyle read(Element e) {
    String s = XMLReader.attribute(e, "font-style");
    if (s != null)
    {
      return InvalidMusicXML.throwNull(Parse.getEnumValue(s, values()), e);
    }

    return null;
  }

  public void write(Element e)
  {
    e.setAttribute("font-style", toString().toLowerCase());
  }
}