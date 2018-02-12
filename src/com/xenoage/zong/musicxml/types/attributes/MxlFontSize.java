package com.xenoage.zong.musicxml.types.attributes;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.zong.musicxml.types.enums.MxlCSSFontSize;
import org.w3c.dom.Element;

public final class MxlFontSize
{
  public static final String ATTR_NAME = "font-size";

  @MaybeNull
  private final Float valuePt;

  @MaybeNull
  private final MxlCSSFontSize valueCSS;

  public MxlFontSize(Float valuePt)
  {
    this.valuePt = valuePt;
    this.valueCSS = null;
  }

  public MxlFontSize(MxlCSSFontSize valueCSS)
  {
    this.valuePt = null;
    this.valueCSS = valueCSS;
  }

  @MaybeNull
  public Float getValuePt()
  {
    return this.valuePt;
  }

  @MaybeNull
  public MxlCSSFontSize getValueCSS()
  {
    return this.valueCSS;
  }

  public boolean isSizePt()
  {
    return this.valuePt != null;
  }

  @MaybeNull
  public static MxlFontSize read(Element e)
  {
    String s = XMLReader.attribute(e, "font-size");
    if (s != null)
    {
      if (Character.isDigit(s.charAt(0)))
      {
        return new MxlFontSize(Float.valueOf(Float.parseFloat(s)));
      }

      return new MxlFontSize(MxlCSSFontSize.read(s));
    }

    return null;
  }

  public void write(Element e)
  {
    e.setAttribute("font-size", this.valuePt != null ? "" + this.valuePt : this.valueCSS.getXMLName());
  }
}