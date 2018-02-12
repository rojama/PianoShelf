package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public final class MxlScaling
{
  public static final String ELEM_NAME = "scaling";
  private final float millimeters;
  private final float tenths;

  public MxlScaling(float millimeters, float tenths)
  {
    this.millimeters = millimeters;
    this.tenths = tenths;
  }

  public float getMillimeters()
  {
    return this.millimeters;
  }

  public float getTenths()
  {
    return this.tenths;
  }

  @NeverNull
  public static MxlScaling read(Element e) {
    return new MxlScaling(Parse.parseChildFloat(e, "millimeters"), Parse.parseChildFloat(e, "tenths"));
  }

  public void write(Element parent)
  {
    Element e = XMLWriter.addElement("scaling", parent);
    XMLWriter.addElement("millimeters", Float.valueOf(this.millimeters), e);
    XMLWriter.addElement("tenths", Float.valueOf(this.tenths), e);
  }
}