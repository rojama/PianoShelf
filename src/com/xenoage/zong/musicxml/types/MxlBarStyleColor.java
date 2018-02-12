package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.attributes.MxlColor;
import com.xenoage.zong.musicxml.types.enums.MxlBarStyle;
import org.w3c.dom.Element;

public final class MxlBarStyleColor
{
  public static final String ELEM_NAME = "bar-style";

  @NeverNull
  private final MxlBarStyle barStyle;

  @MaybeNull
  private final MxlColor color;

  public MxlBarStyleColor(MxlBarStyle barStyle, MxlColor color)
  {
    this.barStyle = barStyle;
    this.color = color;
  }

  @NeverNull
  public MxlBarStyle getBarStyle() {
    return this.barStyle;
  }

  @MaybeNull
  public MxlColor getColor() {
    return this.color;
  }

  @NeverNull
  public static MxlBarStyleColor read(Element e) {
    return new MxlBarStyleColor(MxlBarStyle.read(e), MxlColor.read(e));
  }

  public void write(Element parent)
  {
    Element e = XMLWriter.addElement("bar-style", parent);
    this.barStyle.write(e);
    if (this.color != null)
      this.color.write(e);
  }
}