package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.choice.MxlFullNoteContent;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="display-step-octave")
public final class MxlRest
  implements MxlFullNoteContent
{
  public static final String XML_NAME = "rest";

  @Override
public MxlFullNoteContent.MxlFullNoteContentType getFullNoteContentType()
  {
    return MxlFullNoteContent.MxlFullNoteContentType.Rest;
  }

  @NeverNull
  public static MxlRest read(Element e) {
    return new MxlRest();
  }

  @Override
public void write(Element parent)
  {
    XMLWriter.addElement("rest", parent);
  }
}