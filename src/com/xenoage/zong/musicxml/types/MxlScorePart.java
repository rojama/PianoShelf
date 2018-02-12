package com.xenoage.zong.musicxml.types;

import com.xenoage.pdlib.PVector;
import com.xenoage.util.annotations.MaybeEmpty;
import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.choice.MxlPartListContent;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="part-name-display,part-abbreviation-display,group,midi-device", children="score-instrument,midi-instrument", partly = "")
public class MxlScorePart
  implements MxlPartListContent
{
  public static final String ELEM_NAME = "score-part";

  @MaybeNull
  private final MxlIdentification identification;

  @NeverNull
  private final String partName;

  @MaybeNull
  private final String partAbbreviation;

  @MaybeEmpty
  private final PVector<MxlScoreInstrument> scoreInstruments;

  @MaybeEmpty
  private final PVector<MxlMidiInstrument> midiInstruments;

  @NeverNull
  private final String id;

  public MxlScorePart(MxlIdentification identification, String partName, String partAbbreviation, PVector<MxlScoreInstrument> scoreInstruments, PVector<MxlMidiInstrument> midiInstruments, String id)
  {
    this.identification = identification;
    this.partName = partName;
    this.partAbbreviation = partAbbreviation;
    this.scoreInstruments = scoreInstruments;
    this.midiInstruments = midiInstruments;
    this.id = id;
  }

  @MaybeNull
  public String getID() {
    return this.id;
  }

  @MaybeNull
  public MxlIdentification getIdentification() {
    return this.identification;
  }

  @MaybeNull
  public String getName() {
    return this.partName;
  }

  @MaybeNull
  public String getAbbreviation() {
    return this.partAbbreviation;
  }

  @MaybeEmpty
  public PVector<MxlScoreInstrument> getScoreInstruments() {
    return this.scoreInstruments;
  }

  @MaybeEmpty
  public PVector<MxlMidiInstrument> getMidiInstruments() {
    return this.midiInstruments;
  }

  @Override
public MxlPartListContent.PartListContentType getPartListContentType()
  {
    return MxlPartListContent.PartListContentType.ScorePart;
  }

  @NeverNull
  public static MxlScorePart read(Element e) {
    MxlIdentification identification = null;
    String partName = null;
    String partAbbreviation = null;
    PVector scoreInstruments = PVector.pvec();
    PVector midiInstruments = PVector.pvec();
    for (Element c : XMLReader.elements(e))
    {
      String n = c.getNodeName();
      switch (n.charAt(0))
      {
      case 'i':
        if (!n.equals("identification")) break;
        identification = MxlIdentification.read(c); break;
      case 'm':
        if (!n.equals("midi-instrument")) break;
        midiInstruments = midiInstruments.plus(MxlMidiInstrument.read(c)); break;
      case 'p':
        if (n.equals("part-abbreviation")) {
          partAbbreviation = c.getTextContent(); } else {
          if (!n.equals("part-name")) break;
          partName = c.getTextContent(); } break;
      case 's':
        if (!n.equals("score-instrument")) break;
        scoreInstruments = scoreInstruments.plus(MxlScoreInstrument.read(c));
      }
    }

    return new MxlScorePart(identification, partName, partAbbreviation, scoreInstruments, midiInstruments, InvalidMusicXML.throwNull(XMLReader.attribute(e, "id"), e));
  }

  @Override
public void write(Element parent)
  {
    Element e = XMLWriter.addElement("score-part", parent);
    if (this.identification != null)
      this.identification.write(e);
    XMLWriter.addElement("part-name", this.partName, e);
    XMLWriter.addElement("part-abbreviation", this.partAbbreviation, e);
    for (MxlScoreInstrument scoreInstrument : this.scoreInstruments)
      scoreInstrument.write(e);
    for (MxlMidiInstrument midiInstrument : this.midiInstruments)
      midiInstrument.write(e);
    XMLWriter.addAttribute(e, "id", this.id);
  }
}