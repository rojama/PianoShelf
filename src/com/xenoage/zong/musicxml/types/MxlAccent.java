package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.attributes.MxlEmptyPlacement;
import com.xenoage.zong.musicxml.types.choice.MxlArticulationsContent;
import org.w3c.dom.Element;

public final class MxlAccent
  implements MxlArticulationsContent
{
  public static final String ELEM_NAME = "accent";

  @NeverNull
  private final MxlEmptyPlacement emptyPlacement;
  public static final MxlAccent defaultInstance = new MxlAccent(MxlEmptyPlacement.empty);

  public MxlAccent(MxlEmptyPlacement emptyPlacement)
  {
    this.emptyPlacement = emptyPlacement;
  }

  @NeverNull
  public MxlEmptyPlacement getEmptyPlacement() {
    return this.emptyPlacement;
  }

  @Override
public MxlArticulationsContent.MxlArticulationsContentType getArticulationsContentType()
  {
    return MxlArticulationsContent.MxlArticulationsContentType.Accent;
  }

  @NeverNull
  public static MxlAccent read(Element e) {
    MxlEmptyPlacement emptyPlacement = MxlEmptyPlacement.read(e);
    if (emptyPlacement != MxlEmptyPlacement.empty) {
      return new MxlAccent(emptyPlacement);
    }
    return defaultInstance;
  }

  @Override
public void write(Element parent)
  {
    Element e = XMLWriter.addElement("accent", parent);
    this.emptyPlacement.write(e);
  }
}