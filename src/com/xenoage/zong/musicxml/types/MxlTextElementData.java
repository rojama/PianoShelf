package com.xenoage.zong.musicxml.types;

import com.xenoage.util.xml.XMLReader;
import org.w3c.dom.Element;

public final class MxlTextElementData
{
  private final String value;

  public MxlTextElementData(String value)
  {
    this.value = value;
  }

  public String getValue()
  {
    return this.value;
  }

  public static MxlTextElementData read(Element e)
  {
    return new MxlTextElementData(XMLReader.text(e));
  }

  public void write(Element e)
  {
    e.setTextContent(this.value);
  }
}