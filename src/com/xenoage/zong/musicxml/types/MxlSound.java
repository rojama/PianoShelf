package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="midi-instrument,offset,dynamics,dacapo,segno,dalsegno,coda,tocoda,divisions,forward-repeat,fine,time-only,pizzicato,pan,elevation,damper-pedal,soft-pedal,sostenuto-pedal")
public final class MxlSound
{
  public static final String ELEM_NAME = "sound";
  private final Float tempo;

  public MxlSound(Float tempo)
  {
    this.tempo = tempo;
  }

  @MaybeNull
  public Float getTempo() {
    return this.tempo;
  }

  @NeverNull
  public static MxlSound read(Element e) {
    return new MxlSound(Parse.parseAttrFloatNull(e, "tempo"));
  }

  public void write(Element parent)
  {
    if (this.tempo != null)
    {
      Element e = XMLWriter.addElement("sound", parent);
      XMLWriter.addAttribute(e, "tempo", this.tempo);
    }
  }
}