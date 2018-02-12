package com.xenoage.zong.musicxml.types;

import com.xenoage.pdlib.PVector;
import com.xenoage.util.annotations.MaybeEmpty;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="encoding,source,relation,miscellaneous", children = "", partly = "")
public final class MxlIdentification
{
  public static final String ELEM_NAME = "identification";

  @MaybeEmpty
  private final PVector<MxlTypedText> creators;

  @MaybeEmpty
  private final PVector<MxlTypedText> rights;

  public MxlIdentification(PVector<MxlTypedText> creators, PVector<MxlTypedText> rights)
  {
    this.creators = creators;
    this.rights = rights;
  }

  @MaybeEmpty
  public PVector<MxlTypedText> getCreators() {
    return this.creators;
  }

  @MaybeEmpty
  public PVector<MxlTypedText> getRights() {
    return this.rights;
  }

  public static MxlIdentification read(Element e)
  {
    PVector creators = PVector.pvec();
    PVector rights = PVector.pvec();
    for (Element c : XMLReader.elements(e))
    {
      String n = c.getNodeName();
      if (n.equals("creator"))
        creators = creators.plus(MxlTypedText.read(c));
      else if (n.equals("rights"))
        creators = creators.plus(MxlTypedText.read(c));
    }
    return new MxlIdentification(creators, rights);
  }

  public void write(Element parent)
  {
    Element e = XMLWriter.addElement("identification", parent);
    for (MxlTypedText t : this.creators)
      t.write(e);
    for (MxlTypedText t : this.rights)
      t.write(e);
  }
}