package com.xenoage.zong.musicxml.types.groups;

import com.rojama.pianoshelf.PaintTransfer;
import com.xenoage.pdlib.PVector;
import com.xenoage.util.annotations.MaybeEmpty;
import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.zong.musicxml.types.MxlPageLayout;
import com.xenoage.zong.musicxml.types.MxlStaffLayout;
import com.xenoage.zong.musicxml.types.MxlSystemLayout;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(children="page-layout", missing = "", partly = "")
public final class MxlLayout
{

  @MaybeNull
  private final MxlPageLayout pageLayout;

  @MaybeNull
  private final MxlSystemLayout systemLayout;

  @MaybeEmpty
  private final PVector<MxlStaffLayout> staffLayouts;
  public static final MxlLayout empty = new MxlLayout(null, null, new PVector());

  public MxlLayout(MxlPageLayout pageLayout, MxlSystemLayout systemLayout, PVector<MxlStaffLayout> staffLayouts)
  {
    this.pageLayout = pageLayout;
    this.systemLayout = systemLayout;
    this.staffLayouts = staffLayouts;
  }

  @MaybeNull
  public MxlPageLayout getPageLayout() {
    return this.pageLayout;
  }

  @MaybeNull
  public MxlSystemLayout getSystemLayout() {
    return this.systemLayout;
  }

  @MaybeEmpty
  public PVector<MxlStaffLayout> getStaffLayouts() {
    return this.staffLayouts;
  }

  @NeverNull
  public static MxlLayout read(Element e) {
    MxlPageLayout pageLayout = null;
    MxlSystemLayout systemLayout = null;
    PVector staffLayouts = new PVector();
    for (Element c : XMLReader.elements(e))
    {
      String n = c.getNodeName();
      if (n.equals("page-layout"))
        pageLayout = MxlPageLayout.read(c);
      else if (n.equals("system-layout"))
        systemLayout = MxlSystemLayout.read(c);
      else if (n.equals("staff-layout"))
        staffLayouts = staffLayouts.plus(MxlStaffLayout.read(c));
    }
    if ((pageLayout != null) || (systemLayout != null) || (staffLayouts.size() > 0)) {
      return new MxlLayout(pageLayout, systemLayout, staffLayouts);
    }
    return empty;
  }

  public void write(Element e)
  {
    if (this != empty)
    {
      if (this.pageLayout != null)
        this.pageLayout.write(e);
      if (this.systemLayout != null)
        this.systemLayout.write(e);
      for (MxlStaffLayout staffLayout : this.staffLayouts)
        staffLayout.write(e);
    }
  }
  
  public void paint(PaintTransfer pt) {
	  if (this.getPageLayout()!=null) this.getPageLayout().paint(pt);
	  if (this.getSystemLayout()!=null) this.getSystemLayout().paint(pt);
	  for (MxlStaffLayout staffLayout : this.getStaffLayouts()){
		  staffLayout.paint(pt);
	  }

  }
}