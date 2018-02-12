package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.choice.MxlTimeContent;
import org.w3c.dom.Element;

public final class MxlSenzaMisura
  implements MxlTimeContent
{
  public static final String ELEM_NAME = "senza-misura";

  @Override
public MxlTimeContent.MxlTimeContentType getTimeContentType()
  {
    return MxlTimeContent.MxlTimeContentType.SenzaMisura;
  }

  @NeverNull
  public static MxlSenzaMisura read(Element e) {
    return new MxlSenzaMisura();
  }

  @Override
public void write(Element e)
  {
    XMLWriter.addElement("senza-misura", e);
  }
}