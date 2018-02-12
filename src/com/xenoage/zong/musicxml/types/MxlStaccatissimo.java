package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.attributes.MxlEmptyPlacement;
import com.xenoage.zong.musicxml.types.choice.MxlArticulationsContent;
import org.w3c.dom.Element;

public final class MxlStaccatissimo
  implements MxlArticulationsContent
{
  public static final String ELEM_NAME = "staccatissimo";

  @NeverNull
  private final MxlEmptyPlacement emptyPlacement;
  public static final MxlStaccatissimo defaultInstance = new MxlStaccatissimo(MxlEmptyPlacement.empty);

  public MxlStaccatissimo(MxlEmptyPlacement emptyPlacement)
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
    return MxlArticulationsContent.MxlArticulationsContentType.Staccatissimo;
  }

  @NeverNull
  public static MxlStaccatissimo read(Element e) {
    MxlEmptyPlacement emptyPlacement = MxlEmptyPlacement.read(e);
    if (emptyPlacement != MxlEmptyPlacement.empty) {
      return new MxlStaccatissimo(emptyPlacement);
    }
    return defaultInstance;
  }

  @Override
public void write(Element parent)
  {
    Element e = XMLWriter.addElement("staccatissimo", parent);
    this.emptyPlacement.write(e);
  }
}