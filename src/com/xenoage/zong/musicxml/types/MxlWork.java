package com.xenoage.zong.musicxml.types;

import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="opus")
public final class MxlWork
{
  public static final String ELEM_NAME = "work";
  public static final MxlWork empty = new MxlWork(null, null);

  @MaybeNull
  private final String workNumber;

  @MaybeNull
  private final String workTitle;

  public MxlWork(String workNumber, String workTitle) { this.workNumber = workNumber;
    this.workTitle = workTitle; }

  @MaybeNull
  public String getWorkNumber()
  {
    return this.workNumber;
  }

  @MaybeNull
  public String getWorkTitle() {
    return this.workTitle;
  }

  @NeverNull
  public static MxlWork read(Element e) {
    String workNumber = XMLReader.elementText(e, "work-number");
    String workTitle = XMLReader.elementText(e, "work-title");
    if ((workNumber != null) || (workTitle != null)) {
      return new MxlWork(workNumber, workTitle);
    }
    return empty;
  }

  public void write(Element parent)
  {
    Element e = XMLWriter.addElement("work", parent);
    if (this != empty)
    {
      XMLWriter.addElement("work-number", this.workNumber, e);
      XMLWriter.addElement("work-title", this.workTitle, e);
    }
  }
}