package com.xenoage.zong.musicxml.types;

import com.xenoage.util.NullTools;
import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.attributes.MxlPosition;
import com.xenoage.zong.musicxml.types.choice.MxlNotationsContent;
import com.xenoage.zong.musicxml.types.enums.MxlPlacement;
import com.xenoage.zong.musicxml.types.enums.MxlStartStopContinue;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing = "line-type,orientation,color", children = "", partly = "")
public final class MxlCurvedLine implements MxlNotationsContent {
	public static final String ELEM_NAME_SLUR = "slur";
	public static final String ELEM_NAME_TIED = "tied";

	@NeverNull
	private final MxlElementType elementType;

	@NeverNull
	private final MxlStartStopContinue type;

	@MaybeNull
	private final Integer number;

	@NeverNull
	private final MxlPosition position;

	@MaybeNull
	private final MxlPlacement placement;

	@NeverNull
	private final MxlBezier bezier;
	private static final int defaultNumberForSlur = 1;

	public MxlCurvedLine(MxlElementType elementType, MxlStartStopContinue type, Integer number,
			MxlPosition position, MxlPlacement placement, MxlBezier bezier) {
		this.elementType = elementType;
		this.type = type;
		this.number = number;
		this.position = position;
		this.placement = placement;
		this.bezier = bezier;
	}

	@NeverNull
	public MxlElementType getElementType() {
		return this.elementType;
	}

	@NeverNull
	public MxlStartStopContinue getType() {
		return this.type;
	}

	@MaybeNull
	public Integer getNumber() {
		return this.number;
	}

	@NeverNull
	public MxlPosition getPosition() {
		return this.position;
	}

	@MaybeNull
	public MxlPlacement getPlacement() {
		return this.placement;
	}

	@NeverNull
	public MxlBezier getBezier() {
		return this.bezier;
	}

	@Override
	public MxlNotationsContent.MxlNotationsContentType getNotationsContentType() {
		return MxlNotationsContent.MxlNotationsContentType.CurvedLine;
	}

	@NeverNull
	public static MxlCurvedLine read(Element e) {
		MxlElementType elementType = null;
		String eName = e.getNodeName();
		if ("slur".equals(eName))
			elementType = MxlElementType.Slur;
		else if ("tied".equals(eName))
			elementType = MxlElementType.Tied;
		else {
			throw InvalidMusicXML.invalid(e);
		}
		MxlStartStopContinue type = MxlStartStopContinue.read(XMLReader.attribute(e, "type"), e);
		if ((type == MxlStartStopContinue.Continue) && (elementType == MxlElementType.Tied)) {
			throw InvalidMusicXML.invalid(e);
		}
		Integer number = Parse.parseAttrIntNull(e, "number");
		if (elementType == MxlElementType.Slur)
			number = NullTools.notNull(number, Integer.valueOf(1));
		MxlPosition position = MxlPosition.read(e);
		MxlPlacement placement = MxlPlacement.read(e);
		MxlBezier bezier = MxlBezier.read(e);
		return new MxlCurvedLine(elementType, type, number, position, placement, bezier);
	}

	@Override
	public void write(Element parent) {
		Element e = XMLWriter.addElement(this.elementType == MxlElementType.Slur ? "slur" : "tied",
				parent);

		XMLWriter.addAttribute(e, "type", this.type.write());
		XMLWriter.addAttribute(e, "number", this.number);
		this.position.write(e);
		if (this.placement != null)
			this.placement.write(e);
		this.bezier.write(e);
	}

	public static enum MxlElementType {
		Slur, Tied;
	}

	public int hashCode() {
		return number==null?0:number;
	}
}