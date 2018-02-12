package com.xenoage.zong.musicxml.types;

import com.xenoage.util.NullTools;
import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.choice.MxlPartListContent;
import com.xenoage.zong.musicxml.types.enums.MxlStartStop;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="group-name-display,group-abbreviation-display,group-time,editorial", children = "", partly = "")
public final class MxlPartGroup
  implements MxlPartListContent
{
  public static final String ELEM_NAME = "part-group";

  @MaybeNull
  private final String groupName;

  @MaybeNull
  private final String groupAbbreviation;

  @MaybeNull
  private final MxlGroupSymbol groupSymbol;

  @MaybeNull
  private final MxlGroupBarline groupBarline;

  @NeverNull
  private final MxlStartStop type;
  private final int number;
  private static final int defaultNumber = 1;

  public MxlPartGroup(String groupName, String groupAbbreviation, MxlGroupSymbol groupSymbol, MxlGroupBarline groupBarline, MxlStartStop type, int number)
  {
    this.groupName = groupName;
    this.groupAbbreviation = groupAbbreviation;
    this.groupSymbol = groupSymbol;
    this.groupBarline = groupBarline;
    this.type = type;
    this.number = number;
  }

  @MaybeNull
  public String getGroupName() {
    return this.groupName;
  }

  @MaybeNull
  public String getGroupAbbreviation() {
    return this.groupAbbreviation;
  }

  @MaybeNull
  public MxlGroupSymbol getGroupSymbol() {
    return this.groupSymbol;
  }

  @MaybeNull
  public MxlGroupBarline getGroupBarline() {
    return this.groupBarline;
  }

  @NeverNull
  public MxlStartStop getType() {
    return this.type;
  }

  public int getNumber()
  {
    return this.number;
  }

  @Override
public MxlPartListContent.PartListContentType getPartListContentType()
  {
    return MxlPartListContent.PartListContentType.PartGroup;
  }

  @NeverNull
  public static MxlPartGroup read(Element e) {
    MxlGroupSymbol groupSymbol = null;
    MxlGroupBarline groupBarline = null;
    Element eGroupSymbol = XMLReader.element(e, "group-symbol");
    if (eGroupSymbol != null)
      groupSymbol = MxlGroupSymbol.read(eGroupSymbol);
    Element eGroupBarline = XMLReader.element(e, "group-barline");
    if (eGroupBarline != null)
      groupBarline = MxlGroupBarline.read(eGroupBarline);
    return new MxlPartGroup(XMLReader.elementText(e, "group-name"), XMLReader.elementText(e, "group-abbreviation"), groupSymbol, groupBarline, MxlStartStop.read(InvalidMusicXML.throwNull(XMLReader.attribute(e, "type"), e), e), (NullTools.notNull(Parse.parseAttrIntNull(e, "number"), Integer.valueOf(1))).intValue());
  }

  @Override
public void write(Element parent)
  {
    Element e = XMLWriter.addElement("part-group", parent);
    XMLWriter.addElement("group-name", this.groupName, e);
    XMLWriter.addElement("group-abbreviation", this.groupAbbreviation, e);
    if (this.groupSymbol != null)
      this.groupSymbol.write(e);
    if (this.groupBarline != null)
      this.groupBarline.write(e);
    XMLWriter.addAttribute(e, "type", this.type.write());
    XMLWriter.addAttribute(e, "number", Integer.valueOf(this.number));
  }
}