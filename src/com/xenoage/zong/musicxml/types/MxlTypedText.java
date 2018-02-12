package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import org.w3c.dom.Element;

public final class MxlTypedText
{

  @NeverNull
  private final String value;

  @MaybeNull
  private final String type;

  public MxlTypedText(String value, String type)
  {
    this.value = value;
    this.type = type;
  }

  @NeverNull
  public String getValue() {
    return this.value;
  }

  @MaybeNull
  public String getType() {
    return this.type;
  }

  @NeverNull
  public static MxlTypedText read(Element e) {
    return new MxlTypedText(XMLReader.text(e), XMLReader.attribute(e, "type"));
  }

  public void write(Element e)
  {
    e.setTextContent(this.value);
    XMLWriter.addAttribute(e, "type", this.type);
  }
}