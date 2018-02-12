package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.choice.MxlFullNoteContent;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="display-step-octave")
public final class MxlUnpitched
  implements MxlFullNoteContent
{
  public static final String XML_NAME = "unpitched";

  @Override
public MxlFullNoteContent.MxlFullNoteContentType getFullNoteContentType()
  {
    return MxlFullNoteContent.MxlFullNoteContentType.Unpitched;
  }

  @NeverNull
  public static MxlUnpitched read(Element e) {
    return new MxlUnpitched();
  }

  @Override
public void write(Element parent)
  {
    XMLWriter.addElement("unpitched", parent);
  }
}