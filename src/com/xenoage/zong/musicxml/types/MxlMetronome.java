package com.xenoage.zong.musicxml.types;

import com.xenoage.util.Range;
import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.attributes.MxlPrintStyle;
import com.xenoage.zong.musicxml.types.choice.MxlDirectionTypeContent;
import com.xenoage.zong.musicxml.types.enums.MxlNoteTypeValue;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import java.util.Iterator;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="metronome-note,metronome-relation,parentheses", children = "", partly = "")
public final class MxlMetronome
  implements MxlDirectionTypeContent
{
  public static final String ELEM_NAME = "metronome";

  @NeverNull
  public final MxlNoteTypeValue beatUnit;

  @NeverNull
  public final int dotsCount;

  @NeverNull
  public final int perMinute;

  @NeverNull
  public final MxlPrintStyle printStyle;

  private MxlMetronome(MxlNoteTypeValue beatUnit, int dotsCount, int perMinute, MxlPrintStyle printStyle)
  {
    this.beatUnit = beatUnit;
    this.dotsCount = dotsCount;
    this.perMinute = perMinute;
    this.printStyle = printStyle;
  }

  @Override
public MxlDirectionTypeContent.MxlDirectionTypeContentType getDirectionTypeContentType()
  {
    return MxlDirectionTypeContent.MxlDirectionTypeContentType.Metronome;
  }

  @MaybeNull
  public static MxlMetronome read(Element e)
  {
    String sBeatUnit = XMLReader.elementText(e, "beat-unit");
    int dotsCount = XMLReader.elements(e, "beat-unit-dot").size();
    Integer perMinute = Parse.parseChildIntNull(e, "per-minute");
    MxlPrintStyle printStyle = MxlPrintStyle.read(e);
    if ((sBeatUnit != null) && (perMinute != null))
    {
      MxlNoteTypeValue beatUnit = MxlNoteTypeValue.read(sBeatUnit);
      return new MxlMetronome(beatUnit, dotsCount, perMinute.intValue(), printStyle);
    }

    return null;
  }

  @Override
public void write(Element parent)
  {
    Element e = XMLWriter.addElement("metronome", parent);
    XMLWriter.addElement("beat-unit", this.beatUnit.write(), e);
    for (Iterator i$ = Range.range(this.dotsCount).iterator(); i$.hasNext(); ) { int i = ((Integer)i$.next()).intValue();
      XMLWriter.addElement("beat-unit-dot", e); }
    XMLWriter.addElement("per-minute", Integer.valueOf(this.perMinute), e);
    this.printStyle.write(e);
  }
}