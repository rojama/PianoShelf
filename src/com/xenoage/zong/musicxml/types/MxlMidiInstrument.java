package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="midi-name,midi-bank,midi-unpitched,volume,pan,elevation", children = "", partly = "")
public final class MxlMidiInstrument
{
  public static final String ELEM_NAME = "midi-instrument";

  @MaybeNull
  private final Integer midiChannel;

  @MaybeNull
  private final Integer midiProgram;

  @NeverNull
  private final String id;

  public MxlMidiInstrument(Integer midiChannel, Integer midiProgram, String id)
  {
    this.midiChannel = midiChannel;
    this.midiProgram = midiProgram;
    this.id = id;
  }

  @MaybeNull
  public Integer getMidiChannel() {
    return this.midiChannel;
  }

  @MaybeNull
  public Integer getMidiProgram() {
    return this.midiProgram;
  }

  @NeverNull
  public String getID() {
    return this.id;
  }

  @NeverNull
  public static MxlMidiInstrument read(Element e) {
    return new MxlMidiInstrument(Parse.parseChildIntNull(e, "midi-channel"), Parse.parseChildIntNull(e, "midi-program"), InvalidMusicXML.throwNull(XMLReader.attribute(e, "id"), e));
  }

  public void write(Element parent)
  {
    Element e = XMLWriter.addElement("midi-instrument", parent);
    XMLWriter.addElement("midi-channel", this.midiChannel, e);
    XMLWriter.addElement("midi-program", this.midiProgram, e);
    XMLWriter.addAttribute(e, "id", this.id);
  }
}