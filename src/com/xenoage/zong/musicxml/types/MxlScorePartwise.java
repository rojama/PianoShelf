package com.xenoage.zong.musicxml.types;

import java.util.HashMap;
import java.util.Map;

import android.util.Log;

import com.rojama.pianoshelf.CommonTransfer;
import com.rojama.pianoshelf.PaintTransfer;
import com.xenoage.pdlib.PVector;
import com.xenoage.util.NullTools;
import com.xenoage.util.annotations.NeverEmpty;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.partwise.MxlPart;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@IncompleteMusicXML(children="score-header,part")
public final class MxlScorePartwise
{
  public static final String ELEM_NAME = "score-partwise";

  @NeverNull
  private final MxlScoreHeader scoreHeader;

  @NeverEmpty
  private final PVector<MxlPart> parts;

  @NeverNull
  private final String version;
  private static final String defaultVersion = "1.0";

  public MxlScorePartwise(MxlScoreHeader scoreHeader, PVector<MxlPart> parts, String version)
  {
    this.scoreHeader = scoreHeader;
    this.parts = parts;
    this.version = version;
  }

  @NeverNull
  public MxlScoreHeader getScoreHeader() {
    return this.scoreHeader;
  }

  @NeverEmpty
  public PVector<MxlPart> getParts() {
    return this.parts;
  }

  @NeverNull
  public String getVersion() {
    return this.version;
  }

  @NeverNull
  public static MxlScorePartwise read(Element e) {
    PVector parts = PVector.pvec();
    for (Element c : XMLReader.elements(e))
    {
      if (c.getNodeName().equals("part"))
        parts = parts.plus(MxlPart.read(c));
    }
    if (parts.size() < 1)
      throw InvalidMusicXML.invalid(e);
    return new MxlScorePartwise(MxlScoreHeader.read(e), parts, NullTools.notNull(XMLReader.attribute(e, "version"), "1.0"));
  }

  public Document write()
  {
    Document doc = XMLWriter.createEmptyDocument();
    Element e = doc.createElement("score-partwise");
    doc.appendChild(e);
    this.scoreHeader.write(e);
    for (MxlPart part : this.parts)
      part.write(e);
    XMLWriter.addAttribute(e, "version", this.version);
    return doc;
  }
  
  public void paint(CommonTransfer ct) {
	  ct.scorePartsNotes.clear();
	  ct.oldPaintTransfer.clear();
	  ct.oldPartID = null;
	  
	  PaintTransfer pt = new PaintTransfer();
	  pt.ct = ct;
	  
	  this.getScoreHeader().paint(pt);
	  boolean run = true;
	  while (run){
		  //ct.oldPartID = null;
		  for (MxlPart part : this.parts){
			  run = part.print(ct);
			  //ct.oldPartID = part.getID();
		  }
	  }
	  //´òÓ¡Ò³Âë
	  ct.paint.setTextSize(10);
//	  ct.drawText(ct.getDisPageNo()+"/"+ct.nowPage, ct.getPageWidth()-50, 50);
  }
}