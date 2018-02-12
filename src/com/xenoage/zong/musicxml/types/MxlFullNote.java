package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.choice.MxlFullNoteContent;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(children="unpitched,rest", missing = "")
public final class MxlFullNote
{
  private final boolean chord;

  @NeverNull
  private final MxlFullNoteContent content;

  public MxlFullNote(boolean chord, MxlFullNoteContent content)
  {
    this.chord = chord;
    this.content = content;
  }

  public boolean isChord()
  {
    return this.chord;
  }

  @NeverNull
  public MxlFullNoteContent getContent() {
    return this.content;
  }

  @NeverNull
  public static MxlFullNote read(Element e) {
    boolean chord = false;
    MxlFullNoteContent content = null;
    for (Element c : XMLReader.elements(e))
    {
      String n = c.getNodeName();
      if (n.equals("chord"))
      {
        chord = true;
      } else {
        if (n.equals("pitch"))
        {
          content = MxlPitch.read(c);
          break;
        }
        if (n.equals("unpitched"))
        {
          content = MxlUnpitched.read(c);
          break;
        }
        if (n.equals("rest"))
        {
          content = MxlRest.read(c);
          break;
        }
      }
    }
    InvalidMusicXML.throwNull(content, e);
    return new MxlFullNote(chord, content);
  }

  public void write(Element e)
  {
    if (this.chord)
      XMLWriter.addElement("chord", e);
    this.content.write(e);
  }
}