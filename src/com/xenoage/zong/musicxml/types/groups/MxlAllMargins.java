package com.xenoage.zong.musicxml.types.groups;

import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public final class MxlAllMargins
{
  private final float leftMargin;
  private final float rightMargin;
  private final float topMargin;
  private final float bottomMargin;

  public MxlAllMargins(float leftMargin, float rightMargin, float topMargin, float bottomMargin)
  {
    this.leftMargin = leftMargin;
    this.rightMargin = rightMargin;
    this.topMargin = topMargin;
    this.bottomMargin = bottomMargin;
  }

  public float getLeftMargin()
  {
    return this.leftMargin;
  }

  public float getRightMargin()
  {
    return this.rightMargin;
  }

  public float getTopMargin()
  {
    return this.topMargin;
  }

  public float getBottomMargin()
  {
    return this.bottomMargin;
  }

  @NeverNull
  public static MxlAllMargins read(Element e) {
    return new MxlAllMargins(Parse.parseChildFloat(e, "left-margin"), Parse.parseChildFloat(e, "right-margin"), Parse.parseChildFloat(e, "top-margin"), Parse.parseChildFloat(e, "bottom-margin"));
  }

  public void write(Element e)
  {
    XMLWriter.addElement("left-margin", Float.valueOf(this.leftMargin), e);
    XMLWriter.addElement("right-margin", Float.valueOf(this.rightMargin), e);
    XMLWriter.addElement("top-margin", Float.valueOf(this.topMargin), e);
    XMLWriter.addElement("bottom-margin", Float.valueOf(this.bottomMargin), e);
  }
}