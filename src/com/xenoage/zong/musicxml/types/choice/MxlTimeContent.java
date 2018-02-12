package com.xenoage.zong.musicxml.types.choice;

import org.w3c.dom.Element;

public abstract interface MxlTimeContent
{
  public abstract MxlTimeContentType getTimeContentType();

  public abstract void write(Element paramElement);

  public static enum MxlTimeContentType
  {
    NormalTime, 
    SenzaMisura;
  }
}