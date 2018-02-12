package com.xenoage.zong.musicxml.types.attributes;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.enums.MxlBackwardForward;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public final class MxlRepeat
{
  public static final String ELEM_NAME = "repeat";

  @NeverNull
  private final MxlBackwardForward direction;

  @MaybeNull
  private final Integer times;

  public MxlRepeat(MxlBackwardForward direction, Integer times)
  {
    this.direction = direction;
    this.times = times;
  }

  @NeverNull
  public MxlBackwardForward getDirection() {
    return this.direction;
  }

  @MaybeNull
  public Integer getTimes() {
    return this.times;
  }

  @NeverNull
  public static MxlRepeat read(Element e) {
    return new MxlRepeat(MxlBackwardForward.read(e), Parse.parseAttrIntNull(e, "times"));
  }

  public void write(Element parent)
  {
    Element e = XMLWriter.addElement("repeat", parent);
    this.direction.write(e);
    XMLWriter.addAttribute(e, "times", this.times);
  }
}