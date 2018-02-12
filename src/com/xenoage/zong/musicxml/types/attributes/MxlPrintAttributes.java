package com.xenoage.zong.musicxml.types.attributes;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.enums.MxlYesNo;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="staff-spacing,blank-page,page-number", children = "", partly = "")
public class MxlPrintAttributes
{

  @MaybeNull
  private final Boolean newSystem;

  @MaybeNull
  private final Boolean newPage;
  public static final MxlPrintAttributes empty = new MxlPrintAttributes(null, null);

  public MxlPrintAttributes(Boolean newSystem, Boolean newPage)
  {
    this.newSystem = newSystem;
    this.newPage = newPage;
  }

  @MaybeNull
  public Boolean getNewSystem() {
    return this.newSystem;
  }

  @MaybeNull
  public Boolean getNewPage() {
    return this.newPage;
  }

  @NeverNull
  public static MxlPrintAttributes read(Element e) {
    Boolean newSystem = MxlYesNo.readNull(XMLReader.attribute(e, "new-system"), e);
    Boolean newPage = MxlYesNo.readNull(XMLReader.attribute(e, "new-page"), e);
    if ((newSystem != null) || (newPage != null)) {
      return new MxlPrintAttributes(newSystem, newPage);
    }
    return empty;
  }

  public void write(Element e)
  {
    if (this != empty)
    {
      if (this.newSystem != null)
        XMLWriter.addAttribute(e, "new-system", this.newSystem);
      if (this.newPage != null)
        XMLWriter.addAttribute(e, "new-page", this.newPage);
    }
  }
}