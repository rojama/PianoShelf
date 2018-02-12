package com.xenoage.zong.musicxml.types.attributes;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import org.w3c.dom.Element;

public final class MxlPrintStyle
{

  @NeverNull
  private final MxlPosition position;

  @NeverNull
  private final MxlFont font;

  @MaybeNull
  private final MxlColor color;
  public static final MxlPrintStyle empty = new MxlPrintStyle(MxlPosition.empty, MxlFont.empty, null);

  public MxlPrintStyle(MxlPosition position, MxlFont font, MxlColor color)
  {
    this.position = position;
    this.font = font;
    this.color = color;
  }

  @NeverNull
  public MxlPosition getPosition() {
    return this.position;
  }

  @NeverNull
  public MxlFont getFont() {
    return this.font;
  }

  @MaybeNull
  public MxlColor getColor() {
    return this.color;
  }

  @NeverNull
  public static MxlPrintStyle read(Element e) {
    MxlPosition position = MxlPosition.read(e);
    MxlFont font = MxlFont.read(e);
    MxlColor color = MxlColor.read(e);
    if ((position != MxlPosition.empty) || (font != MxlFont.empty) || (color != null)) {
      return new MxlPrintStyle(position, font, color);
    }
    return empty;
  }

  public void write(Element e)
  {
    if (this != empty)
    {
      this.position.write(e);
      this.font.write(e);
      if (this.color != null)
        this.color.write(e);
    }
  }
}