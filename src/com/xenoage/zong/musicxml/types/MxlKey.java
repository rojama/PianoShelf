package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="non-traditional-key,key-octave,number,print-style,print-object", partly="traditional-key")
public final class MxlKey
{
  public static final String ELEM_NAME = "key";
  private final int fifths;

  public MxlKey(int fifths)
  {
    this.fifths = fifths;
  }

  public int getFifths()
  {
    return this.fifths;
  }

  @MaybeNull
  public static MxlKey read(Element e)
  {
    Integer fifths = Parse.parseChildIntNull(e, "fifths");
    if (fifths != null) {
      return new MxlKey(fifths.intValue());
    }
    return null;
  }

  public void write(Element parent)
  {
    Element e = XMLWriter.addElement("key", parent);
    XMLWriter.addElement("fifths", Integer.valueOf(this.fifths), e);
  }
}