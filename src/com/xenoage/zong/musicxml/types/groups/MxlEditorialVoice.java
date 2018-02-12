package com.xenoage.zong.musicxml.types.groups;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="footnote,level")
public final class MxlEditorialVoice
{

  @MaybeNull
  private final String voice;
  public static final MxlEditorialVoice empty = new MxlEditorialVoice(null);

  public MxlEditorialVoice(String voice)
  {
    this.voice = voice;
  }

  @MaybeNull
  public String getVoice() {
    return this.voice;
  }

  @NeverNull
  public static MxlEditorialVoice read(Element e) {
    String voice = XMLReader.elementText(e, "voice");
    if (voice != null) {
      return new MxlEditorialVoice(voice);
    }
    return empty;
  }

  public void write(Element e)
  {
    if (this != empty)
    {
      if (this.voice != null)
        XMLWriter.addElement("voice", this.voice, e);
    }
  }
}