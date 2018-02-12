package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.choice.MxlTimeContent;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public final class MxlNormalTime
  implements MxlTimeContent
{
  private final int beats;
  private final int beatType;

  public MxlNormalTime(int beats, int beatType)
  {
    this.beats = beats;
    this.beatType = beatType;
  }

  public int getBeats()
  {
    return this.beats;
  }

  public int getBeatType()
  {
    return this.beatType;
  }

  @Override
public MxlTimeContent.MxlTimeContentType getTimeContentType()
  {
    return MxlTimeContent.MxlTimeContentType.NormalTime;
  }

  @NeverNull
  public static MxlNormalTime read(Element e)
  {
    try {
      return new MxlNormalTime(Parse.parseChildInt(e, "beats"), Parse.parseChildInt(e, "beat-type"));
    }
    catch (NumberFormatException ex)
    {
    }
    throw InvalidMusicXML.invalid(e);
  }

  @Override
public void write(Element e)
  {
    XMLWriter.addElement("beats", Integer.valueOf(this.beats), e);
    XMLWriter.addElement("beat-type", Integer.valueOf(this.beatType), e);
  }
}