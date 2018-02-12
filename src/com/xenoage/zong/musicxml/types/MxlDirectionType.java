package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.choice.MxlDirectionTypeContent;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="rehearsal,segno,coda,dashes,bracket,octave-shift,harp-pedals,damp,damp-all,eyeglasses,scordatura,accordion-registration,other-direction")
public final class MxlDirectionType
{
  public static final String ELEM_NAME = "direction-type";

  @NeverNull
  private final MxlDirectionTypeContent content;

  public MxlDirectionType(MxlDirectionTypeContent content)
  {
    this.content = content;
  }

  @NeverNull
  public MxlDirectionTypeContent getContent() {
    return this.content;
  }

  @MaybeNull
  public static MxlDirectionType read(Element e)
  {
    MxlDirectionTypeContent content = null;
    Element firstChild = XMLReader.element(e);
    String n = firstChild.getNodeName();
    switch (n.charAt(0))
    {
    case 'd':
      if (!n.equals("dynamics")) break;
      content = MxlDynamics.read(firstChild); break;
    case 'i':
      if (!n.equals("image")) break;
      content = MxlImage.read(firstChild); break;
    case 'p':
      if (!n.equals("pedal")) break;
      content = MxlPedal.read(firstChild); break;
    case 'm':
      if (!n.equals("metronome")) break;
      content = MxlMetronome.read(firstChild); break;
    case 'w':
      if (n.equals("wedge")) {
        content = MxlWedge.read(firstChild); } else {
        if (!n.equals("words")) break;
        content = MxlWords.read(firstChild);
      }
    }
    if (content != null) {
      return new MxlDirectionType(content);
    }
    return null;
  }

  public void write(Element parent)
  {
    Element e = XMLWriter.addElement("direction-type", parent);
    this.content.write(e);
  }
}