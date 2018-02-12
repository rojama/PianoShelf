package com.xenoage.zong.musicxml.types.groups;

import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public final class MxlLeftRightMargins
{
  private final float leftMargin;
  private final float rightMargin;

  public MxlLeftRightMargins(float leftMargin, float rightMargin)
  {
    this.leftMargin = leftMargin;
    this.rightMargin = rightMargin;
  }

  public float getLeftMargin()
  {
    return this.leftMargin;
  }

  public float getRightMargin()
  {
    return this.rightMargin;
  }

  @NeverNull
  public static MxlLeftRightMargins read(Element e) {
    return new MxlLeftRightMargins(Parse.parseChildFloat(e, "left-margin"), Parse.parseChildFloat(e, "right-margin"));
  }

  public void write(Element e)
  {
    XMLWriter.addElement("left-margin", Float.valueOf(this.leftMargin), e);
    XMLWriter.addElement("right-margin", Float.valueOf(this.rightMargin), e);
  }
}