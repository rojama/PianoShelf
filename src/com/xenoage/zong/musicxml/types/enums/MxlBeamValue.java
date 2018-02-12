package com.xenoage.zong.musicxml.types.enums;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public enum MxlBeamValue
  implements EnumWithXMLNames
{
  Begin("begin"), 
  Continue("continue"), 
  End("end"), 
  ForwardHook("forward hook"), 
  BackwardHook("backward hook");

  private final String xmlName;

  private MxlBeamValue(String xmlName)
  {
    this.xmlName = xmlName;
  }

  @Override
public String getXMLName()
  {
    return this.xmlName;
  }

  @MaybeNull
  public static MxlBeamValue read(Element e) {
    return Parse.getEnumValueNamed(e.getTextContent(), values());
  }

  public void write(Element e)
  {
    e.setTextContent(this.xmlName);
  }
}