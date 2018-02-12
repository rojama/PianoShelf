package com.xenoage.zong.musicxml.types;

import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.choice.MxlLyricContent;
import org.w3c.dom.Element;

public final class MxlExtend
  implements MxlLyricContent
{
  public static final String ELEM_NAME = "extend";

  @Override
public MxlLyricContent.MxlLyricContentType getLyricContentType()
  {
    return MxlLyricContent.MxlLyricContentType.Extend;
  }

  public static MxlExtend read(Element e)
  {
    return new MxlExtend();
  }

  @Override
public void write(Element parent)
  {
    XMLWriter.addElement("extend", parent);
  }
}