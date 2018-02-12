package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.attributes.MxlFont;
import org.w3c.dom.Element;

public final class MxlLyricFont
{
  public static final String ELEM_NAME = "lyric-font";

  @MaybeNull
  private final String number;

  @MaybeNull
  private final String name;

  @NeverNull
  private final MxlFont font;

  public MxlLyricFont(String number, String name, MxlFont font)
  {
    this.number = number;
    this.name = name;
    this.font = font;
  }

  @MaybeNull
  public String getNumber() {
    return this.number;
  }

  @MaybeNull
  public String getName() {
    return this.name;
  }

  @NeverNull
  public MxlFont getFont() {
    return this.font;
  }

  @NeverNull
  public static MxlLyricFont read(Element e) {
    return new MxlLyricFont(XMLReader.attribute(e, "number"), XMLReader.attribute(e, "name"), MxlFont.read(e));
  }

  public void write(Element parent)
  {
    Element e = XMLWriter.addElement("lyric-font", parent);
    XMLWriter.addAttribute(e, "number", this.number);
    XMLWriter.addAttribute(e, "name", this.name);
    this.font.write(e);
  }
}