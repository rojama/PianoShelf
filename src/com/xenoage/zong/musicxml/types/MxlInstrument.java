package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import org.w3c.dom.Element;

public final class MxlInstrument
{
  public static final String ELEM_NAME = "instrument";

  @NeverNull
  private final String id;

  public MxlInstrument(String id)
  {
    this.id = id;
  }

  @NeverNull
  public String getID() {
    return this.id;
  }

  @NeverNull
  public static MxlInstrument read(Element e) {
    return new MxlInstrument(InvalidMusicXML.throwNull(XMLReader.attribute(e, "id"), e));
  }

  public void write(Element parent)
  {
    Element e = XMLWriter.addElement("instrument", parent);
    XMLWriter.addAttribute(e, "id", this.id);
  }
}