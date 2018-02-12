package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.attributes.MxlColor;
import com.xenoage.zong.musicxml.types.enums.MxlGroupBarlineValue;
import org.w3c.dom.Element;

public final class MxlGroupBarline
{
  public static final String ELEM_NAME = "group-barline";

  @NeverNull
  private final MxlGroupBarlineValue value;

  @MaybeNull
  private final MxlColor color;

  public MxlGroupBarline(MxlGroupBarlineValue value, MxlColor color)
  {
    this.value = value;
    this.color = color;
  }

  @NeverNull
  public MxlGroupBarlineValue getValue() {
    return this.value;
  }

  @MaybeNull
  public MxlColor getColor() {
    return this.color;
  }

  @NeverNull
  public static MxlGroupBarline read(Element e) {
    return new MxlGroupBarline(MxlGroupBarlineValue.read(e), MxlColor.read(e));
  }

  public void write(Element parent)
  {
    Element e = XMLWriter.addElement("group-barline", parent);
    this.value.write(e);
    if (this.color != null)
      this.color.write(e);
  }
}