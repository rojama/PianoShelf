package com.xenoage.zong.musicxml.types.choice;

import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="laughing,humming", children = "", partly = "")
public abstract interface MxlLyricContent
{
  public abstract MxlLyricContentType getLyricContentType();

  public abstract void write(Element paramElement);

  public static enum MxlLyricContentType
  {
    SyllabicText, 
    Extend;
  }
}