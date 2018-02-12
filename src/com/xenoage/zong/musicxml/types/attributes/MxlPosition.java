package com.xenoage.zong.musicxml.types.attributes;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public final class MxlPosition
{

  @MaybeNull
  private final Float defaultX;

  @MaybeNull
  private final Float defaultY;

  @MaybeNull
  private final Float relativeX;

  @MaybeNull
  private final Float relativeY;
  public static final MxlPosition empty = new MxlPosition(null, null, null, null);

  public MxlPosition(Float defaultX, Float defaultY, Float relativeX, Float relativeY)
  {
    this.defaultX = defaultX;
    this.defaultY = defaultY;
    this.relativeX = relativeX;
    this.relativeY = relativeY;
  }

  @MaybeNull
  public Float getDefaultX() {
    return this.defaultX;
  }

  @MaybeNull
  public Float getDefaultY() {
    return this.defaultY;
  }

  @MaybeNull
  public Float getRelativeX() {
    return this.relativeX;
  }

  @MaybeNull
  public Float getRelativeY() {
    return this.relativeY;
  }

  @NeverNull
  public static MxlPosition read(Element e) {
    Float defaultX = Parse.parseAttrFloatNull(e, "default-x");
    Float defaultY = Parse.parseAttrFloatNull(e, "default-y");
    Float relativeX = Parse.parseAttrFloatNull(e, "relative-x");
    Float relativeY = Parse.parseAttrFloatNull(e, "relative-y");
    if ((defaultX != null) || (defaultY != null) || (relativeX != null) || (relativeY != null))
    {
      return new MxlPosition(defaultX, defaultY, relativeX, relativeY);
    }

    return empty;
  }

  public void write(Element e)
  {
    if (this != empty)
    {
      XMLWriter.addAttribute(e, "default-x", this.defaultX);
      XMLWriter.addAttribute(e, "default-y", this.defaultY);
      XMLWriter.addAttribute(e, "relative-x", this.relativeX);
      XMLWriter.addAttribute(e, "relative-y", this.relativeY);
    }
  }
}