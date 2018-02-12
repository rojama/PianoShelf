package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.enums.MxlMarginType;
import com.xenoage.zong.musicxml.types.groups.MxlAllMargins;
import org.w3c.dom.Element;

public final class MxlPageMargins
{
  public static final String ELEM_NAME = "page-margins";

  @NeverNull
  private final MxlAllMargins value;

  @MaybeNull
  private final MxlMarginType type;

  public MxlPageMargins(MxlAllMargins value, MxlMarginType type)
  {
    this.value = value;
    this.type = type;
  }

  @NeverNull
  public MxlAllMargins getValue() {
    return this.value;
  }

  @MaybeNull
  public MxlMarginType getType() {
    return this.type;
  }

  public float getLeftMargin()
  {
    return this.value.getLeftMargin();
  }

  public float getRightMargin()
  {
    return this.value.getRightMargin();
  }

  public float getTopMargin()
  {
    return this.value.getTopMargin();
  }

  public float getBottomMargin()
  {
    return this.value.getBottomMargin();
  }

  @NeverNull
  public MxlMarginType getTypeNotNull()
  {
    return this.type != null ? this.type : MxlMarginType.Both;
  }

  @NeverNull
  public static MxlPageMargins read(Element e) {
    return new MxlPageMargins(MxlAllMargins.read(e), MxlMarginType.read(e));
  }

  public void write(Element parent)
  {
    Element e = XMLWriter.addElement("page-margins", parent);
    this.value.write(e);
    if (this.type != null)
      this.type.write(e);
  }
}