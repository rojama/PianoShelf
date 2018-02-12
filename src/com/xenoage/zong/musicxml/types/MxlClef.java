package com.xenoage.zong.musicxml.types;

import com.xenoage.util.NullTools;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.enums.MxlClefSign;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="line,additional,size,print-style,print-object")
public final class MxlClef
{
  public static final String ELEM_NAME = "clef";

  @NeverNull
  private final MxlClefSign sign;
  private final int clefOctaveChange;
  private final int number;
  private static final int defaultClefOctaveChange = 0;
  private static final int defaultNumber = 1;

  public MxlClef(MxlClefSign sign, int clefOctaveChange, int number)
  {
    this.sign = sign;
    this.clefOctaveChange = clefOctaveChange;
    this.number = number;
  }

  @NeverNull
  public MxlClefSign getSign() {
    return this.sign;
  }

  public int getClefOctaveChange()
  {
    return this.clefOctaveChange;
  }

  public int getNumber()
  {
    return this.number;
  }

  public static MxlClef read(Element e)
  {
    return new MxlClef(MxlClefSign.read(InvalidMusicXML.throwNull(XMLReader.element(e, "sign"), e)), (NullTools.notNull(Parse.parseChildIntNull(e, "clef-octave-change"), Integer.valueOf(0))).intValue(), (NullTools.notNull(Parse.parseAttrIntNull(e, "number"), Integer.valueOf(1))).intValue());
  }

  public void write(Element parent)
  {
    Element e = XMLWriter.addElement("clef", parent);
    this.sign.write(e);
    XMLWriter.addElement("clef-octave-change", Integer.valueOf(this.clefOctaveChange), e);
    XMLWriter.addAttribute(e, "number", Integer.valueOf(this.number));
  }
}