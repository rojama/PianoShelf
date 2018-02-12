package com.xenoage.zong.musicxml.types;

import com.rojama.pianoshelf.PaintTransfer;
import com.xenoage.util.NullTools;
import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.attributes.MxlRepeat;
import com.xenoage.zong.musicxml.types.choice.MxlMusicDataContent;
import com.xenoage.zong.musicxml.types.enums.MxlRightLeftMiddle;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing = "editorial,wavy-line,segno,coda,fermata,ending,segno,coda,divisions")
public final class MxlBarline implements MxlMusicDataContent {
	public static final String ELEM_NAME = "barline";

	@MaybeNull
	private final MxlBarStyleColor barStyle;

	@MaybeNull
	private final MxlRepeat repeat;

	@NeverNull
	private final MxlRightLeftMiddle location;
	private static final MxlRightLeftMiddle defaultLocation = MxlRightLeftMiddle.Right;

	public MxlBarline(MxlBarStyleColor barStyle, MxlRepeat repeat, MxlRightLeftMiddle location) {
		this.barStyle = barStyle;
		this.repeat = repeat;
		this.location = location;
	}

	@MaybeNull
	public MxlBarStyleColor getBarStyle() {
		return this.barStyle;
	}

	@MaybeNull
	public MxlRepeat getRepeat() {
		return this.repeat;
	}

	@NeverNull
	public MxlRightLeftMiddle getLocation() {
		return this.location;
	}

	@Override
	public MxlMusicDataContent.MxlMusicDataContentType getMusicDataContentType() {
		return MxlMusicDataContent.MxlMusicDataContentType.Barline;
	}

	public static MxlBarline read(Element e) {
		MxlBarStyleColor barStyle = null;
		MxlRepeat repeat = null;
		for (Element c : XMLReader.elements(e)) {
			String n = c.getNodeName();
			if (n.equals("bar-style"))
				barStyle = MxlBarStyleColor.read(c);
			else if (n.equals("repeat"))
				repeat = MxlRepeat.read(c);
		}
		MxlRightLeftMiddle location = NullTools
				.notNull(MxlRightLeftMiddle.read(e), defaultLocation);
		return new MxlBarline(barStyle, repeat, location);
	}

	@Override
	public void write(Element parent) {
		Element e = XMLWriter.addElement("barline", parent);
		if (this.barStyle != null)
			this.barStyle.write(e);
		if (this.repeat != null)
			this.repeat.write(e);
		this.location.write(e);
	}

	@Override
	public void print(PaintTransfer pt) {
		float x = 0;
		switch (this.getLocation()) {
		case Middle:
			x = pt.measureLeft + pt.measureWidth / 2;
			break;
		case Left:
			x = pt.measureLeft;
			break;
		case Right:
			x = pt.measureLeft + pt.measureWidth;
			break;
		}
		if (this.getBarStyle() != null) {
			switch (this.getBarStyle().getBarStyle()) {
			case Regular:
			case Dotted:
			case Dashed:
			case Tick:
			case Short:
			case None:
				break;// TODO
			case Heavy:
				pt.drawLine(x - 1, pt.measureUp, x - 1, pt.measureUp + 40);
				break;
			case LightLight:
				pt.drawLine(x - 5, pt.measureUp, x - 5, pt.measureUp + 40);
				break;
			case LightHeavy:
				pt.drawLine(x - 5, pt.measureUp, x - 5, pt.measureUp + 40);
				pt.drawLine(x - 1, pt.measureUp, x - 1, pt.measureUp + 40);
				pt.drawLine(x + 1, pt.measureUp, x + 1, pt.measureUp + 41);
				break;
			case HeavyLight:
				pt.drawLine(x - 6, pt.measureUp, x - 6, pt.measureUp + 40);
				pt.drawLine(x - 5, pt.measureUp, x - 5, pt.measureUp + 40);
				pt.drawLine(x - 4, pt.measureUp, x - 4, pt.measureUp + 40);
				break;
			case HeavyHeavy:
				pt.drawLine(x - 6, pt.measureUp, x - 6, pt.measureUp + 40);
				pt.drawLine(x - 5, pt.measureUp, x - 5, pt.measureUp + 40);
				pt.drawLine(x - 4, pt.measureUp, x - 4, pt.measureUp + 40);
				pt.drawLine(x - 1, pt.measureUp, x - 1, pt.measureUp + 40);
				pt.drawLine(x + 1, pt.measureUp, x + 1, pt.measureUp + 41);
				break;
			}
		}
	}
}