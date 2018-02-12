package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.choice.MxlNoteContent;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

@IncompleteMusicXML(children="full-note", missing = "", partly = "")
public final class MxlCueNote
  implements MxlNoteContent
{

  @NeverNull
  private final MxlFullNote fullNote;
  private final int duration;

  public MxlCueNote(MxlFullNote fullNote, int duration)
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
    return MxlNoteContent.MxlNoteContentType.Cue;
  }

  @NeverNull
  public static MxlCueNote read(Element e) {
    return new MxlCueNote(MxlFullNote.read(e), Parse.parseChildInt(e, "duration"));
  }

  @Override
public void write(Element e)
  {
    XMLWriter.addElement("cue", e);
    this.fullNote.write(e);
    XMLWriter.addElement("duration", Integer.valueOf(this.duration), e);
  }
}