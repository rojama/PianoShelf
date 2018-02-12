package com.xenoage.zong.musicxml.types.choice;

import org.w3c.dom.Element;

public abstract interface MxlCreditContent
{
  public abstract MxlCreditContentType getCreditContentType();

  public abstract void write(Element paramElement);

  public static enum MxlCreditContentType
  {
    CreditImage, 
    CreditWords;
  }
}