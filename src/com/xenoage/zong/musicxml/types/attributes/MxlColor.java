package com.xenoage.zong.musicxml.types.attributes;

import android.graphics.Color;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import org.w3c.dom.Element;

public final class MxlColor
{
  public static final String ATTR_NAME = "color";

  @NeverNull
  private final int value;

  public MxlColor(int value)
  {
    this.value = value;
  }

  @NeverNull
  public int getValue() {
    return this.value;
  }

  @MaybeNull
  public static MxlColor read(Element e) {
    String s = XMLReader.attribute(e, "color");
    if (s != null)
    {
      try
      {
        return new MxlColor(Color.parseColor(s));
      }
      catch (NumberFormatException ex)
      {
        throw InvalidMusicXML.invalid(e);
      }

    }else{
    	return null;
    }
  }

  public void write(Element e)
  {
    e.setAttribute("color", "#"+Integer.toHexString(this.value));
  }
}