package com.xenoage.zong.musicxml.types.enums;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public enum MxlStemValue
{
  Down, 
  Up, 
  Double, 
  None;

  @MaybeNull
  public static MxlStemValue read(Element e) {
    String s = XMLReader.text(e);
    return InvalidMusicXML.throwNull(Parse.getEnumValue(s, values()), e);
  }

  public void write(Element e)
  {
    e.setTextContent(toString().toLowerCase());
  }
}