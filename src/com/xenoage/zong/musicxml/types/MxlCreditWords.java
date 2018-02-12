package com.xenoage.zong.musicxml.types;

import com.rojama.pianoshelf.PaintTransfer;
import com.xenoage.pdlib.PVector;
import com.xenoage.util.annotations.NeverEmpty;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.choice.MxlCreditContent;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="link,bookmark", partly="credit-words")
public final class MxlCreditWords
  implements MxlCreditContent
{

  @NeverEmpty
  private final PVector<MxlFormattedText> items;

  public MxlCreditWords(PVector<MxlFormattedText> items)
  {
    this.items = items;
  }

  @NeverEmpty
  public PVector<MxlFormattedText> getItems() {
    return this.items;
  }

  @Override
public MxlCreditContent.MxlCreditContentType getCreditContentType()
  {
    return MxlCreditContent.MxlCreditContentType.CreditWords;
  }

  @NeverNull
  public static MxlCreditWords read(Element parent) {
    PVector items = PVector.pvec();
    for (Element e : XMLReader.elements(parent))
    {
      String n = e.getNodeName();
      if (n.equals("credit-words"))
        items = items.plus(MxlFormattedText.read(e));
    }
    if (items.size() < 1)
      throw InvalidMusicXML.invalid(parent);
    return new MxlCreditWords(items);
  }

  @Override
public void write(Element parent)
  {
    for (MxlFormattedText item : this.items)
    {
      Element e = XMLWriter.addElement("credit-words", parent);
      item.write(e);
    }
  }
  
  public void paint(PaintTransfer pt){
	  for (MxlFormattedText item : this.items)
	    {
	      item.paint(pt);
	    }
  }
}