package com.xenoage.zong.musicxml.types;

import com.xenoage.pdlib.PVector;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.choice.MxlNotationsContent;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="tuplet,glissando,slide,ornaments,technical,fermata,arpeggiate,non-arpeggiate,accidental-mark,other-notation", children="slur,tied,articulations,dynamics")
public final class MxlNotations
{
  public static final String ELEM_NAME = "notations";
  private final PVector<MxlNotationsContent> elements;

  public MxlNotations(PVector<MxlNotationsContent> elements)
  {
    this.elements = elements;
  }

  public PVector<MxlNotationsContent> getElements()
  {
    return this.elements;
  }

  public static MxlNotations read(Element e)
  {
    PVector elements = PVector.pvec();
    for (Element child : XMLReader.elements(e))
    {
      String childName = child.getNodeName();
      MxlNotationsContent element = null;
      if (childName.equals("articulations"))
      {
        element = MxlArticulations.read(child);
      }
      else if (childName.equals("dynamics"))
      {
        element = MxlDynamics.read(child);
      }
      else if ((childName.equals("slur")) || (childName.equals("tied")))
      {
        element = MxlCurvedLine.read(child);
      }
      if (element != null)
      {
        elements = elements.plus(element);
      }
    }
    return new MxlNotations(elements);
  }

  public void write(Element parent)
  {
    Element e = XMLWriter.addElement("notations", parent);
    for (MxlNotationsContent element : this.elements)
    {
      element.write(e);
    }
  }
}