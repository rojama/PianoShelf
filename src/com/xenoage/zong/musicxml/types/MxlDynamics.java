package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.core.music.direction.DynamicsType;
import com.xenoage.zong.musicxml.types.attributes.MxlPrintStyle;
import com.xenoage.zong.musicxml.types.choice.MxlDirectionTypeContent;
import com.xenoage.zong.musicxml.types.choice.MxlNotationsContent;
import com.xenoage.zong.musicxml.types.enums.MxlPlacement;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import java.util.ArrayList;
import org.w3c.dom.Element;

@IncompleteMusicXML(partly="")
public final class MxlDynamics
  implements MxlNotationsContent, MxlDirectionTypeContent
{
  public static final String ELEM_NAME = "dynamics";

  @NeverNull
  private final DynamicsType element;

  @NeverNull
  private final MxlPrintStyle printStyle;

  @MaybeNull
  private final MxlPlacement placement;

  public MxlDynamics(DynamicsType element, MxlPrintStyle printStyle, MxlPlacement placement)
  {
    this.element = element;
    this.printStyle = printStyle;
    this.placement = placement;
  }

  @NeverNull
  public DynamicsType getElement() {
    return this.element;
  }

  @NeverNull
  public MxlPrintStyle getPrintStyle() {
    return this.printStyle;
  }

  @MaybeNull
  public MxlPlacement getPlacement() {
    return this.placement;
  }

  @Override
public MxlNotationsContent.MxlNotationsContentType getNotationsContentType()
  {
    return MxlNotationsContent.MxlNotationsContentType.Dynamics;
  }

  @Override
public MxlDirectionTypeContent.MxlDirectionTypeContentType getDirectionTypeContentType()
  {
    return MxlDirectionTypeContent.MxlDirectionTypeContentType.Dynamics;
  }

  @MaybeNull
  public static MxlDynamics read(Element e)
  {
    String childText = ((Element)((ArrayList)InvalidMusicXML.throwNull(XMLReader.elements(e), e)).get(0)).getTagName();
    DynamicsType element = null;
    for (DynamicsType type : DynamicsType.values())
    {
      if (!type.name().equals(childText))
        continue;
      element = type;
      break;
    }

    if (element != null)
    {
      return new MxlDynamics(element, MxlPrintStyle.read(e), MxlPlacement.read(e));
    }

    return null;
  }

  @Override
public void write(Element parent)
  {
    Element e = XMLWriter.addElement("dynamics", parent);
    XMLWriter.addElement(this.element.name(), e);
    this.printStyle.write(e);
    if (this.placement != null)
      this.placement.write(e);
  }
}