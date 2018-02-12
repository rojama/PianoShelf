package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.choice.MxlLyricContent;
import com.xenoage.zong.musicxml.types.enums.MxlSyllabic;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import java.util.List;
import org.w3c.dom.Element;

@IncompleteMusicXML(partly="")
public final class MxlSyllabicText
  implements MxlLyricContent
{

  @NeverNull
  private final MxlSyllabic syllabic;

  @NeverNull
  private final MxlTextElementData text;
  private static final MxlSyllabic defaultSyllabic = MxlSyllabic.Single;

  public MxlSyllabicText(MxlSyllabic syllabic, MxlTextElementData text)
  {
    this.syllabic = syllabic;
    this.text = text;
  }

  @NeverNull
  public MxlSyllabic getSyllabic()
  {
    return this.syllabic;
  }

  @NeverNull
  public MxlTextElementData getText() {
    return this.text;
  }

  @Override
public MxlLyricContent.MxlLyricContentType getLyricContentType()
  {
    return MxlLyricContent.MxlLyricContentType.SyllabicText;
  }

  public static MxlSyllabicText read(Element e)
  {
    List elements = XMLReader.elements(e);
    int index = 0;

    MxlSyllabic syllabic = defaultSyllabic;
    Element firstChild = (Element)elements.get(0);
    if (firstChild.getNodeName().equals("syllabic"))
    {
      syllabic = InvalidMusicXML.throwNull(Parse.getEnumValue(XMLReader.text(firstChild), MxlSyllabic.values()), firstChild);
      index++;
    }

    Element eText = (Element)elements.get(index);
    if (!eText.getNodeName().equals("text"))
      throw InvalidMusicXML.invalid(e);
    MxlTextElementData text = MxlTextElementData.read(eText);
    return new MxlSyllabicText(syllabic, text);
  }

  @Override
public void write(Element parent)
  {
    this.syllabic.write(parent);
    Element eText = XMLWriter.addElement("text", parent);
    this.text.write(eText);
  }
}