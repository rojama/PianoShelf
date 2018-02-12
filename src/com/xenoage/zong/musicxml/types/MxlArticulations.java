package com.xenoage.zong.musicxml.types;

import com.xenoage.pdlib.PVector;
import com.xenoage.util.annotations.MaybeEmpty;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.choice.MxlArticulationsContent;
import com.xenoage.zong.musicxml.types.choice.MxlNotationsContent;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="detached-legato,spiccato,scoop,plop,doit,falloff,breath-mark,caesura,stress,unstress,other-articulation")
public final class MxlArticulations
  implements MxlNotationsContent
{
  public static final String ELEM_NAME = "articulations";

  @MaybeEmpty
  private final PVector<MxlArticulationsContent> content;

  public MxlArticulations(PVector<MxlArticulationsContent> content)
  {
    this.content = content;
  }

  @MaybeEmpty
  public PVector<MxlArticulationsContent> getContent() {
    return this.content;
  }

  @Override
public MxlNotationsContent.MxlNotationsContentType getNotationsContentType()
  {
    return MxlNotationsContent.MxlNotationsContentType.Articulations;
  }

  public static MxlArticulations read(Element e)
  {
    PVector content = PVector.pvec();
    for (Element c : XMLReader.elements(e))
    {
      String n = c.getNodeName();
      switch (n.charAt(0))
      {
      case 'a':
        if (!n.equals("accent")) break;
        content = content.plus(MxlAccent.read(c)); break;
      case 's':
        if (n.equals("strong-accent")) {
          content = content.plus(MxlStrongAccent.read(c));
        } else if (n.equals("staccato")) {
          content = content.plus(MxlStaccato.read(c)); } else {
          if (!n.equals("staccatissimo")) break;
          content = content.plus(MxlStaccatissimo.read(c)); } break;
      case 't':
        if (!n.equals("tenuto")) break;
        content = content.plus(MxlTenuto.read(c));
      }
    }

    return new MxlArticulations(content);
  }

  @Override
public void write(Element parent)
  {
    Element e = XMLWriter.addElement("articulations", parent);
    for (MxlArticulationsContent item : this.content)
      item.write(e);
  }
}