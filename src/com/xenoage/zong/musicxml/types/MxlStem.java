package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.attributes.MxlColor;
import com.xenoage.zong.musicxml.types.attributes.MxlPosition;
import com.xenoage.zong.musicxml.types.enums.MxlStemValue;
import org.w3c.dom.Element;

public final class MxlStem
{
  public static final String ELEM_NAME = "stem";

  @NeverNull
  private final MxlStemValue value;

  @NeverNull
  private final MxlPosition yPosition;

  @MaybeNull
  private final MxlColor color;

  public MxlStem(MxlStemValue value, MxlPosition yPosition, MxlColor color)
  {
    this.value = value;
    this.yPosition = yPosition;
    this.color = color;
  }

  @NeverNull
  public MxlStemValue getValue() {
    return this.value;
  }

  @NeverNull
  public MxlPosition getYPosition() {
    return this.yPosition;
  }

  @MaybeNull
  public MxlColor getColor() {
    return this.color;
  }

  @NeverNull
  public static MxlStem read(Element e) {
    return new MxlStem(MxlStemValue.read(e), MxlPosition.read(e), MxlColor.read(e));
  }

  public void write(Element parent)
  {
    Element e = XMLWriter.addElement("stem", parent);
    this.value.write(e);
    this.yPosition.write(e);
    if (this.color != null)
      this.color.write(e);
  }
}