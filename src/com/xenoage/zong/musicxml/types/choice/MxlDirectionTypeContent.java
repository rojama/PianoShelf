package com.xenoage.zong.musicxml.types.choice;

import org.w3c.dom.Element;

public abstract interface MxlDirectionTypeContent
{
  public abstract MxlDirectionTypeContentType getDirectionTypeContentType();

  public abstract void write(Element paramElement);

  public static enum MxlDirectionTypeContentType
  {
    Words, 
    Wedge, 
    Dynamics, 
    Pedal, 
    Metronome, 
    Image;
  }
}