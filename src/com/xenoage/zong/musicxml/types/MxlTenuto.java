package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.attributes.MxlEmptyPlacement;
import com.xenoage.zong.musicxml.types.choice.MxlArticulationsContent;
import org.w3c.dom.Element;

public final class MxlTenuto
  implements MxlArticulationsContent
{
  public static final String ELEM_NAME = "tenuto";

  @NeverNull
  private final MxlEmptyPlacement emptyPlacement;
  public static final MxlTenuto defaultInstance = new MxlTenuto(MxlEmptyPlacement.empty);

  public MxlTenuto(MxlEmptyPlacement emptyPlacement)
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
    return MxlArticulationsContent.MxlArticulationsContentType.Tenuto;
  }

  @NeverNull
  public static MxlTenuto read(Element e) {
    MxlEmptyPlacement emptyPlacement = MxlEmptyPlacement.read(e);
    if (emptyPlacement != MxlEmptyPlacement.empty) {
      return new MxlTenuto(emptyPlacement);
    }
    return defaultInstance;
  }

  @Override
public void write(Element parent)
  {
    Element e = XMLWriter.addElement("tenuto", parent);
    this.emptyPlacement.write(e);
  }
}