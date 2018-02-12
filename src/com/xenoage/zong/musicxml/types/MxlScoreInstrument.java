package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="solo,ensemble")
public final class MxlScoreInstrument
{
  public static final String ELEM_NAME = "score-instrument";

  @NeverNull
  private final String instrumentName;

  @MaybeNull
  private final String instrumentAbbreviation;

  @NeverNull
  private final String id;

  public MxlScoreInstrument(String instrumentName, String instrumentAbbreviation, String id)
  {
    this.instrumentName = instrumentName;
    this.instrumentAbbreviation = instrumentAbbreviation;
    this.id = id;
  }

  @NeverNull
  public String getInstrumentName() {
    return this.instrumentName;
  }

  @MaybeNull
  public String getInstrumentAbbreviation() {
    return this.instrumentAbbreviation;
  }

  @NeverNull
  public String getID() {
    return this.id;
  }

  @NeverNull
  public static MxlScoreInstrument read(Element e) {
    return new MxlScoreInstrument(InvalidMusicXML.throwNull(XMLReader.elementText(e, "instrument-name"), e), XMLReader.elementText(e, "instrument-abbreviation"), InvalidMusicXML.throwNull(XMLReader.attribute(e, "id"), e));
  }

  public void write(Element parent)
  {
    Element e = XMLWriter.addElement("score-instrument", parent);
    XMLWriter.addElement("instrument-name", this.instrumentName, e);
    XMLWriter.addElement("instrument-abbreviation", this.instrumentAbbreviation, e);
    XMLWriter.addAttribute(e, "id", this.id);
  }
}