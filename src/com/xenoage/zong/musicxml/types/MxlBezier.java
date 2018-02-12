package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public final class MxlBezier
{

  @MaybeNull
  private final Float bezierOffset;

  @MaybeNull
  private final Float bezierOffset2;

  @MaybeNull
  private final Float bezierX;

  @MaybeNull
  private final Float bezierY;

  @MaybeNull
  private final Float bezierX2;

  @MaybeNull
  private final Float bezierY2;
  public static final MxlBezier empty = new MxlBezier(null, null, null, null, null, null);

  public MxlBezier(Float bezierOffset, Float bezierOffset2, Float bezierX, Float bezierY, Float bezierX2, Float bezierY2)
  {
    this.bezierOffset = bezierOffset;
    this.bezierOffset2 = bezierOffset2;
    this.bezierX = bezierX;
    this.bezierY = bezierY;
    this.bezierX2 = bezierX2;
    this.bezierY2 = bezierY2;
  }

  @MaybeNull
  public Float getBezierOffset() {
    return this.bezierOffset;
  }

  @MaybeNull
  public Float getBezierOffset2() {
    return this.bezierOffset2;
  }

  @MaybeNull
  public Float getBezierX() {
    return this.bezierX;
  }

  @MaybeNull
  public Float getBezierY() {
    return this.bezierY;
  }

  @MaybeNull
  public Float getBezierX2() {
    return this.bezierX2;
  }

  @MaybeNull
  public Float getBezierY2() {
    return this.bezierY2;
  }

  @NeverNull
  public static MxlBezier read(Element e) {
    Float bezierOffset = Parse.parseAttrFloatNull(e, "bezier-offset");
    Float bezierOffset2 = Parse.parseAttrFloatNull(e, "bezier-offset-2");
    Float bezierX = Parse.parseAttrFloatNull(e, "bezier-x");
    Float bezierY = Parse.parseAttrFloatNull(e, "bezier-y");
    Float bezierX2 = Parse.parseAttrFloatNull(e, "bezier-x2");
    Float bezierY2 = Parse.parseAttrFloatNull(e, "bezier-y2");
    if ((bezierOffset != null) || (bezierOffset2 != null) || (bezierX != null) || (bezierY != null) || (bezierX2 != null) || (bezierY2 != null))
    {
      return new MxlBezier(bezierOffset, bezierOffset2, bezierX, bezierY, bezierX2, bezierY2);
    }
    return empty;
  }

  public void write(Element e)
  {
    XMLWriter.addAttribute(e, "bezier-offset", this.bezierOffset);
    XMLWriter.addAttribute(e, "bezier-offset-2", this.bezierOffset2);
    XMLWriter.addAttribute(e, "bezier-x", this.bezierX);
    XMLWriter.addAttribute(e, "bezier-y", this.bezierY);
    XMLWriter.addAttribute(e, "bezier-x2", this.bezierX2);
    XMLWriter.addAttribute(e, "bezier-y2", this.bezierY2);
  }
}