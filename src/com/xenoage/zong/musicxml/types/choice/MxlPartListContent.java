package com.xenoage.zong.musicxml.types.choice;

import org.w3c.dom.Element;

public abstract interface MxlPartListContent
{
  public abstract PartListContentType getPartListContentType();

  public abstract void write(Element paramElement);

  public static enum PartListContentType
  {
    PartGroup, 
    ScorePart;
  }
}