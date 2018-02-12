package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.choice.MxlNoteContent;
import com.xenoage.zong.musicxml.types.enums.MxlYesNo;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="tie,steal-time-previous,steal-time-following,make-time", children="full-note")
public final class MxlGraceNote
  implements MxlNoteContent
{

  @NeverNull
  private final MxlFullNote fullNote;

  @MaybeNull
  private final Boolean slash;

  public MxlGraceNote(MxlFullNote fullNote, Boolean slash)
  {
    this.fullNote = fullNote;
    this.slash = slash;
  }

  @Override
@NeverNull
  public MxlFullNote getFullNote() {
    return this.fullNote;
  }

  @MaybeNull
  public Boolean getSlash() {
    return this.slash;
  }

  @Override
public MxlNoteContent.MxlNoteContentType getNoteContentType()
  {
    return MxlNoteContent.MxlNoteContentType.Grace;
  }

  @NeverNull
  public static MxlGraceNote read(Element e) {
    return new MxlGraceNote(MxlFullNote.read(e), MxlYesNo.readNull(XMLReader.attribute(e, "slash"), e));
  }

  @Override
public void write(Element e)
  {
    Element child = XMLWriter.addElement("grace", e);
    if (this.slash != null)
      XMLWriter.addAttribute(child, "slash", MxlYesNo.write(this.slash.booleanValue()));
    this.fullNote.write(e);
  }
}