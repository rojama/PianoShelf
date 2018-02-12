package com.xenoage.zong.musicxml.types.partwise;

import java.util.HashMap;
import java.util.Map;

import android.util.Log;

import com.rojama.pianoshelf.CommonTransfer;
import com.rojama.pianoshelf.PaintTransfer;
import com.xenoage.pdlib.PVector;
import com.xenoage.util.annotations.NeverEmpty;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.MxlStaffLayout;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(children = "measure", missing = "", partly = "")
public final class MxlPart {
	public static final String ELEM_NAME = "part";

	@NeverEmpty
	private final PVector<MxlMeasure> measures;

	@NeverNull
	private final String id;

	public MxlPart(PVector<MxlMeasure> measures, String id) {
		this.measures = measures;
		this.id = id;
	}

	@NeverEmpty
	public PVector<MxlMeasure> getMeasures() {
		return this.measures;
	}

	@NeverNull
	public String getID() {
		return this.id;
	}

	public static MxlPart read(Element e) {
		PVector measures = PVector.pvec();
		for (Element c : XMLReader.elements(e)) {
			if (c.getNodeName().equals("measure"))
				measures = measures.plus(MxlMeasure.read(c));
		}
		if (measures.size() < 1)
			throw InvalidMusicXML.invalid(e);
		return new MxlPart(measures, InvalidMusicXML.throwNull(XMLReader.attribute(e, "id"), e));
	}

	public void write(Element parent) {
		Element e = XMLWriter.addElement("part", parent);
		for (MxlMeasure measure : this.measures)
			measure.write(e);
		XMLWriter.addAttribute(e, "id", this.id);
	}	
	
	public boolean print(CommonTransfer ct) {
//		ct.oldX = 0;
//		ct.oldY = 0;
//		ct.measureLeft = 0;
//		ct.measureUp = 0;
//		ct.measureWidth = 0;		
//		
		 
		PaintTransfer pt;
		if (!ct.oldPaintTransfer.containsKey(id)) {
			pt = new PaintTransfer();
			pt.ct = ct;
			pt.nowPartID = this.id;
			for (MxlStaffLayout staffLayout : ct.defaults.getLayout().getStaffLayouts()) {
				pt.staffLayout.put(staffLayout.getNumber(), staffLayout.getStaffDistance());
			}
			ct.oldPaintTransfer.put(this.id, pt);
		}else{
			pt = ct.oldPaintTransfer.get(this.id);
		}
		
//		if (!pt.oldLine.containsKey(id)) pt.oldLine.put(this.id, 1);
//		if (!pt.oldMeasure.containsKey(id)) pt.oldMeasure.put(this.id, 0);
//		if (!pt.oldPage.containsKey(id)) pt.oldPage.put(this.id, 1);
		
//		pt.nowMeasure = pt.oldMeasure.get(this.id);
//		pt.nowLine = pt.oldLine.get(this.id);
//		pt.nowPage = pt.oldPage.get(this.id);
		
		int temp = 1;
		pt.block = false;
		pt.firstIn = true;		
		
//		Log.d("Part", this.getID());		  
		
		for (MxlMeasure measure : this.measures){
			if (temp++ <= pt.nowMeasure) continue;
			measure.print(pt);
			if (pt.block){
				break;
			}else{
				pt.firstIn = false;
//				pt.oldLine.put(this.id, pt.nowLine);
//				pt.oldMeasure.put(this.id, pt.nowMeasure);
//				pt.oldPage.put(this.id, pt.nowPage);
				//pt.oldPaintTransfer.put(this.id, pt);
			}
		}
		
		ct.oldPartID = this.id;
		return pt.nowMeasure < this.measures.size();
	}
}