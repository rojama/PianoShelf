package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.attributes.MxlEmptyPlacement;
import com.xenoage.zong.musicxml.types.choice.MxlArticulationsContent;
import org.w3c.dom.Element;

public final class MxlStaccato
  implements MxlArticulationsContent
{
  public static final String ELEM_NAME = "staccato";

  @NeverNull
  private final MxlEmptyPlacement emptyPlacement;
  public static final MxlStaccato defaultInstance = new MxlStaccato(MxlEmptyPlacement.empty);

  public MxlStaccato(MxlEmptyPlacement emptyPlacement)
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
    return MxlArticulationsContent.MxlArticulationsContentType.Staccato;
  }

  @NeverNull
  public static MxlStaccato read(Element e) {
    MxlEmptyPlacement emptyPlacement = MxlEmptyPlacement.read(e);
    if (emptyPlacement != MxlEmptyPlacement.empty) {
      return new MxlStaccato(emptyPlacement);
    }
    return defaultInstance;
  }

  @Override
public void write(Element parent)
  {
    Element e = XMLWriter.addElement("staccato", parent);
    this.emptyPlacement.write(e);
  }
}