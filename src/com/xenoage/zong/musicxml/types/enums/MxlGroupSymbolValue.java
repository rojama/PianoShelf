package com.xenoage.zong.musicxml.types.enums;

import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public enum MxlGroupSymbolValue
{
  None, 
  Brace, 
  Line, 
  Bracket;

  @NeverNull
  public static MxlGroupSymbolValue read(Element e) {
    String s = XMLReader.text(e);
    if (s != null)
    {
      return InvalidMusicXML.throwNull(Parse.getEnumValue(s, values()), e);
    }

    throw InvalidMusicXML.invalid(e);
  }

  public void write(Element e)
  {
    e.setTextContent(toString().toLowerCase());
  }
}