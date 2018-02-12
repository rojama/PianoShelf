package com.xenoage.zong.musicxml.types.enums;

import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public enum MxlSyllabic
{
  Single, 
  Begin, 
  End, 
  Middle;

  public static final String ELEM_NAME = "syllabic";

  @NeverNull
  public static MxlSyllabic read(Element e) {
    return InvalidMusicXML.throwNull(Parse.getEnumValue(XMLReader.text(e), values()), e);
  }

  public void write(Element parent)
  {
    XMLWriter.addElement("syllabic", toString().toLowerCase(), parent);
  }
}