package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.attributes.MxlPrintStyle;
import com.xenoage.zong.musicxml.types.choice.MxlDirectionTypeContent;
import com.xenoage.zong.musicxml.types.enums.MxlStartStopChange;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="line")
public final class MxlPedal
  implements MxlDirectionTypeContent
{
  public static final String ELEM_NAME = "pedal";

  @NeverNull
  private final MxlStartStopChange type;

  @NeverNull
  private final MxlPrintStyle printStyle;

  public MxlPedal(MxlStartStopChange type, MxlPrintStyle printStyle)
  {
    this.type = type;
    this.printStyle = printStyle;
  }

  @NeverNull
  public MxlStartStopChange getType() {
    return this.type;
  }

  @NeverNull
  public MxlPrintStyle getPrintStyle() {
    return this.printStyle;
  }

  @Override
public MxlDirectionTypeContent.MxlDirectionTypeContentType getDirectionTypeContentType()
  {
    return MxlDirectionTypeContent.MxlDirectionTypeContentType.Pedal;
  }

  @NeverNull
  public static MxlPedal read(Element e) {
    return new MxlPedal(MxlStartStopChange.read(XMLReader.attribute(e, "type"), e), MxlPrintStyle.read(e));
  }

  @Override
public void write(Element parent)
  {
    Element e = XMLWriter.addElement("pedal", parent);
    e.setAttribute("type", this.type.write());
    this.printStyle.write(e);
  }
}