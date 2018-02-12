package com.xenoage.zong.musicxml.types.choice;

import org.w3c.dom.Element;

public abstract interface MxlNotationsContent
{
  public abstract MxlNotationsContentType getNotationsContentType();

  public abstract void write(Element paramElement);

  public static enum MxlNotationsContentType
  {
    Articulations, 
    Dynamics, 

    CurvedLine;
  }
}