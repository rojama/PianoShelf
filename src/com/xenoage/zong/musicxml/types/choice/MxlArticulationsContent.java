package com.xenoage.zong.musicxml.types.choice;

import org.w3c.dom.Element;

public abstract interface MxlArticulationsContent
{
  public abstract MxlArticulationsContentType getArticulationsContentType();

  public abstract void write(Element paramElement);

  public static enum MxlArticulationsContentType
  {
    Accent, 
    StrongAccent, 
    Staccato, 
    Tenuto, 
    Staccatissimo;
  }
}