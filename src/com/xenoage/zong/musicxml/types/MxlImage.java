package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.attributes.MxlPosition;
import com.xenoage.zong.musicxml.types.choice.MxlCreditContent;
import com.xenoage.zong.musicxml.types.choice.MxlDirectionTypeContent;
import com.xenoage.zong.musicxml.types.enums.MxlLeftCenterRight;
import com.xenoage.zong.musicxml.types.enums.MxlVAlignImage;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import org.w3c.dom.Element;

public final class MxlImage
  implements MxlCreditContent, MxlDirectionTypeContent
{
  private final String elemName;

  @NeverNull
  private final String source;

  @NeverNull
  private final String type;

  @NeverNull
  private final MxlPosition position;

  @MaybeNull
  private final MxlLeftCenterRight hAlign;

  @MaybeNull
  private final MxlVAlignImage vAlign;

  public MxlImage(String elemName, String source, String type, MxlPosition position, MxlLeftCenterRight hAlign, MxlVAlignImage vAlign)
  {
    this.elemName = elemName;
    this.source = source;
    this.type = type;
    this.position = position;
    this.hAlign = hAlign;
    this.vAlign = vAlign;
  }

  @NeverNull
  public String getSource() {
    return this.source;
  }

  @NeverNull
  public String getType() {
    return this.type;
  }

  @MaybeNull
  public MxlPosition getPosition() {
    return this.position;
  }

  @MaybeNull
  public MxlLeftCenterRight getHAlign() {
    return this.hAlign;
  }

  @MaybeNull
  public MxlVAlignImage getVAlign() {
    return this.vAlign;
  }

  @Override
public MxlCreditContent.MxlCreditContentType getCreditContentType()
  {
    return MxlCreditContent.MxlCreditContentType.CreditImage;
  }

  @Override
public MxlDirectionTypeContent.MxlDirectionTypeContentType getDirectionTypeContentType()
  {
    return MxlDirectionTypeContent.MxlDirectionTypeContentType.Image;
  }

  @NeverNull
  public static MxlImage read(Element e) {
    return new MxlImage(e.getNodeName(), InvalidMusicXML.throwNull(XMLReader.attribute(e, "source"), e), InvalidMusicXML.throwNull(XMLReader.attribute(e, "type"), e), MxlPosition.read(e), MxlLeftCenterRight.read(e, "halign"), MxlVAlignImage.read(e));
  }

  @Override
public void write(Element parent)
  {
    Element e = XMLWriter.addElement(this.elemName, parent);
    XMLWriter.addAttribute(e, "source", this.source);
    XMLWriter.addAttribute(e, "type", this.type);
    if (this.position != null)
      this.position.write(e);
    if (this.hAlign != null)
      this.hAlign.write(e, "halign");
    if (this.vAlign != null)
      this.vAlign.write(e);
  }
}