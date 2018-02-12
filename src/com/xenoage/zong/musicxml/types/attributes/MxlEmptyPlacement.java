package com.xenoage.zong.musicxml.types.attributes;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.zong.musicxml.types.enums.MxlPlacement;
import org.w3c.dom.Element;

public final class MxlEmptyPlacement
{

  @NeverNull
  private final MxlPrintStyle printStyle;

  @MaybeNull
  private final MxlPlacement placement;
  public static final MxlEmptyPlacement empty = new MxlEmptyPlacement(MxlPrintStyle.empty, null);

  public MxlEmptyPlacement(MxlPrintStyle printStyle, MxlPlacement placement)
  {
    this.printStyle = printStyle;
    this.placement = placement;
  }

  @NeverNull
  public MxlPrintStyle getPrintStyle() {
    return this.printStyle;
  }

  @MaybeNull
  public MxlPlacement getPlacement() {
    return this.placement;
  }

  @NeverNull
  public static MxlEmptyPlacement read(Element e) {
    MxlPrintStyle printStyle = MxlPrintStyle.read(e);
    MxlPlacement placement = MxlPlacement.read(e);
    if ((printStyle != MxlPrintStyle.empty) || (placement != null)) {
      return new MxlEmptyPlacement(printStyle, placement);
    }
    return empty;
  }

  public void write(Element e)
  {
    if (this != empty)
    {
      this.printStyle.write(e);
      if (this.placement != null)
        this.placement.write(e);
    }
  }
}