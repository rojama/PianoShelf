package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.choice.MxlNoteContent;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="tie", children = "", partly = "")
public final class MxlNormalNote
  implements MxlNoteContent
{

  @NeverNull
  private final MxlFullNote fullNote;
  private final int duration;

  public MxlNormalNote(MxlFullNote fullNote, int duration)
  {
    this.fullNote = fullNote;
    this.duration = duration;
  }

  @Override
@NeverNull
  public MxlFullNote getFullNote() {
    return this.fullNote;
  }

  public int getDuration()
  {
    return this.duration;
  }

  @Override
public MxlNoteContent.MxlNoteContentType getNoteContentType()
  {
    return MxlNoteContent.MxlNoteContentType.Normal;
  }

  @NeverNull
  public static MxlNormalNote read(Element e) {
    return new MxlNormalNote(MxlFullNote.read(e), Parse.parseChildInt(e, "duration"));
  }

  @Override
public void write(Element e)
  {
    this.fullNote.write(e);
    XMLWriter.addElement("duration", Integer.valueOf(this.duration), e);
  }
}