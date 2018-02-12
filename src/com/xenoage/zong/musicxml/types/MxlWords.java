package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.choice.MxlDirectionTypeContent;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(children="formatted-text")
public final class MxlWords
  implements MxlDirectionTypeContent
{
  public static final String ELEM_NAME = "words";

  @NeverNull
  private final MxlFormattedText formattedText;

  public MxlWords(MxlFormattedText formattedText)
  {
    this.formattedText = formattedText;
  }

  @NeverNull
  public MxlFormattedText getFormattedText() {
    return this.formattedText;
  }

  @Override
public MxlDirectionTypeContent.MxlDirectionTypeContentType getDirectionTypeContentType()
  {
    return MxlDirectionTypeContent.MxlDirectionTypeContentType.Words;
  }

  @NeverNull
  public static MxlWords read(Element e) {
    return new MxlWords(MxlFormattedText.read(e));
  }

  @Override
public void write(Element parent)
  {
    Element e = XMLWriter.addElement("words", parent);
    this.formattedText.write(e);
  }
}