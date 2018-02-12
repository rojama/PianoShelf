package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public final class MxlTranspose
{
  public static final String ELEM_NAME = "transpose";

  @MaybeNull
  private final Integer diatonic;
  private final int chromatic;

  @MaybeNull
  private final Integer octaveChange;
  private final boolean doubleValue;

  public MxlTranspose(Integer diatonic, int chromatic, Integer octaveChange, boolean doubleValue)
  {
    this.diatonic = diatonic;
    this.chromatic = chromatic;
    this.octaveChange = octaveChange;
    this.doubleValue = doubleValue;
  }

  @MaybeNull
  public Integer getDiatonic() {
    return this.diatonic;
  }

  public int getChromatic()
  {
    return this.chromatic;
  }

  @MaybeNull
  public Integer getOctaveChange() {
    return this.octaveChange;
  }

  public boolean getDouble()
  {
    return this.doubleValue;
  }

  @NeverNull
  public static MxlTranspose read(Element e) {
    return new MxlTranspose(Parse.parseChildIntNull(e, "diatonic"), Parse.parseChildInt(e, "chromatic"), Parse.parseChildIntNull(e, "octave-change"), XMLReader.element(e, "double") != null);
  }

  public void write(Element parent)
  {
    Element e = XMLWriter.addElement("transpose", parent);
    XMLWriter.addElement("diatonic", this.diatonic, e);
    XMLWriter.addElement("chromatic", Integer.valueOf(this.chromatic), e);
    XMLWriter.addElement("octave-change", this.octaveChange, e);
    if (this.doubleValue) XMLWriter.addElement("double", e);
  }
}