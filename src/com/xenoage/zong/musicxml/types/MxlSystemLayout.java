package com.xenoage.zong.musicxml.types;

import android.util.Log;

import com.rojama.pianoshelf.PaintTransfer;
import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.groups.MxlLeftRightMargins;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

public final class MxlSystemLayout {
	public static final String ELEM_NAME = "system-layout";

	@MaybeNull
	private final MxlLeftRightMargins systemMargins;

	@MaybeNull
	private final Float systemDistance;

	@MaybeNull
	private final Float topSystemDistance;

	public MxlSystemLayout(MxlLeftRightMargins systemMargins, Float systemDistance,
			Float topSystemDistance) {
		this.systemMargins = systemMargins;
		this.systemDistance = systemDistance;
		this.topSystemDistance = topSystemDistance;
	}

	@MaybeNull
	public MxlLeftRightMargins getSystemMargins() {
		return this.systemMargins;
	}

	@MaybeNull
	public Float getSystemDistance() {
		return this.systemDistance;
	}

	@MaybeNull
	public Float getTopSystemDistance() {
		return this.topSystemDistance;
	}

	@NeverNull
	public static MxlSystemLayout read(Element e) {
		MxlLeftRightMargins systemMargins = null;
		Float systemDistance = null;
		Float topSystemDistance = null;
		for (Element c : XMLReader.elements(e)) {
			String n = c.getNodeName();
			if (n.equals("system-margins"))
				systemMargins = MxlLeftRightMargins.read(c);
			else if (n.equals("system-distance"))
				systemDistance = Float.valueOf(Parse.parseFloat(c));
			else if (n.equals("top-system-distance"))
				topSystemDistance = Float.valueOf(Parse.parseFloat(c));
		}
		return new MxlSystemLayout(systemMargins, systemDistance, topSystemDistance);
	}

	public void write(Element parent) {
		Element e = XMLWriter.addElement("system-layout", parent);
		if (this.systemMargins != null)
			this.systemMargins.write(e);
		if (this.systemDistance != null)
			XMLWriter.addElement("system-distance", this.systemDistance, e);
		if (this.topSystemDistance != null)
			XMLWriter.addElement("top-system-distance", this.topSystemDistance, e);
	}

	public void paint(PaintTransfer pt) {
		// ===== 修复 tenths → px：XML 的 system-distance 是 tenths =====
		float sc = (pt.ct.tenthsToPx > 0f) ? pt.ct.tenthsToPx : 1f;
		if (this.getTopSystemDistance() != null) {
			pt.ct.systemTopDistance = this.getTopSystemDistance() * sc;
		} else if (pt.ct.defaults.getLayout().getSystemLayout() != null) {
			Float v = pt.ct.defaults.getLayout().getSystemLayout().getTopSystemDistance();
			pt.ct.systemTopDistance = (v == null) ? 0 : (v * sc);
		}
		if (this.getSystemDistance() != null) {
			pt.ct.systemDistance = this.getSystemDistance() * sc;
		} else if (pt.ct.defaults.getLayout().getSystemLayout() != null) {
			Float v = pt.ct.defaults.getLayout().getSystemLayout().getSystemDistance();
			pt.ct.systemDistance = (v == null) ? 0 : (v * sc);
		}
		if (this.getSystemMargins() != null) {
			pt.ct.systemLeftMargin = this.getSystemMargins().getLeftMargin() * sc;
			pt.ct.systemRightMargin = this.getSystemMargins().getRightMargin() * sc;
		} else if (pt.ct.defaults.getLayout().getSystemLayout() != null){
			if (pt.ct.defaults.getLayout().getSystemLayout().getSystemMargins() != null) {
				pt.ct.systemLeftMargin = pt.ct.defaults.getLayout().getSystemLayout().getSystemMargins()
						.getLeftMargin() * sc;
				pt.ct.systemRightMargin = pt.ct.defaults.getLayout().getSystemLayout().getSystemMargins()
						.getRightMargin() * sc;
			}
		}
	}
}