package com.xenoage.zong.musicxml.types;

import com.xenoage.util.NullTools;
import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.attributes.MxlColor;
import com.xenoage.zong.musicxml.types.attributes.MxlPosition;
import com.xenoage.zong.musicxml.types.choice.MxlDirectionTypeContent;
import com.xenoage.zong.musicxml.types.enums.MxlWedgeType;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="spread")
public final class MxlWedge
  implements MxlDirectionTypeContent
{
  public static final String ELEM_NAME = "wedge";

  @NeverNull
  private final MxlWedgeType type;
  private final int number;

  @NeverNull
  private final MxlPosition position;

  @MaybeNull
  private final MxlColor color;
  private static final int defaultNumber = 1;

  public MxlWedge(MxlWedgeType type, int number, MxlPosition position, MxlColor color)
  {
    this.type = type;
    this.number = number;
    this.position = position;
    this.color = color;
  }

  @NeverNull
  public MxlWedgeType getType() {
    return this.type;
  }

  public int getNumber()
  {
    return this.number;
  }

  @NeverNull
  public MxlPosition getPosition() {
    return this.position;
  }

  @MaybeNull
  public MxlColor getColor() {
    return this.color;
  }

  @Override
public MxlDirectionTypeContent.MxlDirectionTypeContentType getDirectionTypeContentType()
  {
    return MxlDirectionTypeContent.MxlDirectionTypeContentType.Wedge;
  }

  @NeverNull
  public static MxlWedge read(Element e) {
    return new MxlWedge(MxlWedgeType.read(XMLReader.attribute(e, "type"), e), (NullTools.notNull(Parse.parseAttrIntNull(e, "number"), Integer.valueOf(1))).intValue(), MxlPosition.read(e), MxlColor.read(e));
  }

  @Override
public void write(Element parent)
  {
    Element e = XMLWriter.addElement("wedge", parent);
    XMLWriter.addAttribute(e, "type", this.type.write());
    XMLWriter.addAttribute(e, "number", Integer.valueOf(this.number));
    this.position.write(e);
    if (this.color != null)
      this.color.write(e);
  }
}