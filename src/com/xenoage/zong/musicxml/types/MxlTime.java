package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.choice.MxlTimeContent;
import com.xenoage.zong.musicxml.types.enums.MxlTimeSymbol;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="number,print-style,print-object", partly="")
public final class MxlTime
{
  public static final String ELEM_NAME = "time";

  @NeverNull
  private final MxlTimeContent content;

  @MaybeNull
  private final MxlTimeSymbol symbol;

  public MxlTime(MxlTimeContent content, MxlTimeSymbol symbol)
  {
    this.content = content;
    this.symbol = symbol;
  }

  @NeverNull
  public MxlTimeContent getContent() {
    return this.content;
  }

  @MaybeNull
  public MxlTimeSymbol getSymbol() {
    return this.symbol;
  }

  @NeverNull
  public static MxlTime read(Element e) {
    MxlTimeContent content = null;
    Element firstChild = XMLReader.element(e);
    String n = firstChild.getNodeName();
    if (n.equals("beats"))
      content = MxlNormalTime.read(e);
    else if (n.equals("senza-misura"))
      content = MxlSenzaMisura.read(e);
    return new MxlTime(InvalidMusicXML.throwNull(content, e), MxlTimeSymbol.read(e));
  }

  public void write(Element parent)
  {
    Element e = XMLWriter.addElement("time", parent);
    this.content.write(e);
    if (this.symbol != null)
      this.symbol.write(e);
  }
}