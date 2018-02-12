package com.xenoage.zong.musicxml.types;

import com.xenoage.util.NullTools;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.attributes.MxlEmptyPlacement;
import com.xenoage.zong.musicxml.types.choice.MxlArticulationsContent;
import com.xenoage.zong.musicxml.types.enums.MxlUpDown;
import org.w3c.dom.Element;

public final class MxlStrongAccent
  implements MxlArticulationsContent
{
  public static final String ELEM_NAME = "strong-accent";

  @NeverNull
  private final MxlEmptyPlacement emptyPlacement;

  @NeverNull
  private final MxlUpDown type;
  private static final MxlUpDown defaultType = MxlUpDown.Up;
  public static final MxlStrongAccent defaultInstance = new MxlStrongAccent(MxlEmptyPlacement.empty, defaultType);

  public MxlStrongAccent(MxlEmptyPlacement emptyPlacement, MxlUpDown type)
  {
    this.emptyPlacement = emptyPlacement;
    this.type = type;
  }

  @NeverNull
  public MxlEmptyPlacement getEmptyPlacement() {
    return this.emptyPlacement;
  }

  @NeverNull
  public MxlUpDown getType() {
    return this.type;
  }

  @Override
public MxlArticulationsContent.MxlArticulationsContentType getArticulationsContentType()
  {
    return MxlArticulationsContent.MxlArticulationsContentType.StrongAccent;
  }

  @NeverNull
  public static MxlStrongAccent read(Element e) {
    MxlEmptyPlacement emptyPlacement = MxlEmptyPlacement.read(e);
    MxlUpDown type = NullTools.notNull(MxlUpDown.read(XMLReader.attribute(e, "type"), e), defaultType);
    if ((emptyPlacement != MxlEmptyPlacement.empty) || (type != defaultType)) {
      return new MxlStrongAccent(emptyPlacement, type);
    }
    return defaultInstance;
  }

  @Override
public void write(Element parent)
  {
    Element e = XMLWriter.addElement("strong-accent", parent);
    this.emptyPlacement.write(e);
    XMLWriter.addAttribute(e, "type", this.type.write());
  }
}