package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.attributes.MxlColor;
import com.xenoage.zong.musicxml.types.attributes.MxlPosition;
import com.xenoage.zong.musicxml.types.enums.MxlGroupSymbolValue;
import org.w3c.dom.Element;

public final class MxlGroupSymbol
{
  public static final String ELEM_NAME = "group-symbol";

  @NeverNull
  private final MxlGroupSymbolValue value;

  @NeverNull
  private final MxlPosition position;

  @MaybeNull
  private final MxlColor color;

  public MxlGroupSymbol(MxlGroupSymbolValue value, MxlPosition position, MxlColor color)
  {
    this.value = value;
    this.position = position;
    this.color = color;
  }

  @NeverNull
  public MxlGroupSymbolValue getValue() {
    return this.value;
  }

  @NeverNull
  public MxlPosition getPosition() {
    return this.position;
  }

  @MaybeNull
  public MxlColor getColor() {
    return this.color;
  }

  @NeverNull
  public static MxlGroupSymbol read(Element e) {
    return new MxlGroupSymbol(MxlGroupSymbolValue.read(e), MxlPosition.read(e), MxlColor.read(e));
  }

  public void write(Element parent)
  {
    Element e = XMLWriter.addElement("group-symbol", parent);
    this.value.write(e);
    this.position.write(e);
    if (this.color != null)
      this.color.write(e);
  }
}