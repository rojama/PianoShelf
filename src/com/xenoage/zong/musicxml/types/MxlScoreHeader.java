package com.xenoage.zong.musicxml.types;

import com.rojama.pianoshelf.PaintTransfer;
import com.xenoage.pdlib.PVector;
import com.xenoage.util.annotations.MaybeEmpty;
import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(children = "work,identification,defaults,part-list")
public final class MxlScoreHeader {

	@MaybeNull
	private final MxlWork work;

	@MaybeNull
	private final String movementNumber;

	@MaybeNull
	private final String movementTitle;

	@MaybeNull
	private final MxlIdentification identification;

	@MaybeNull
	private final MxlDefaults defaults;

	@MaybeEmpty
	private final PVector<MxlCredit> credits;

	@NeverNull
	private final MxlPartList partList;

	public MxlScoreHeader(MxlWork work, String movementNumber, String movementTitle,
			MxlIdentification identification, MxlDefaults defaults, PVector<MxlCredit> credits,
			MxlPartList partList) {
		this.work = work;
		this.movementNumber = movementNumber;
		this.movementTitle = movementTitle;
		this.identification = identification;
		this.defaults = defaults;
		this.credits = credits;
		this.partList = partList;
	}

	@MaybeNull
	public MxlWork getWork() {
		return this.work;
	}

	@MaybeNull
	public String getMovementNumber() {
		return this.movementNumber;
	}

	@MaybeNull
	public String getMovementTitle() {
		return this.movementTitle;
	}

	@MaybeNull
	public MxlIdentification getIdentification() {
		return this.identification;
	}

	@MaybeNull
	public MxlDefaults getDefaults() {
		return this.defaults;
	}

	@MaybeEmpty
	public PVector<MxlCredit> getCredits() {
		return this.credits;
	}

	@NeverNull
	public MxlPartList getPartList() {
		return this.partList;
	}

	@NeverNull
	public static MxlScoreHeader read(Element e) {
		MxlWork work = null;
		String movementNumber = null;
		String movementTitle = null;
		MxlIdentification identification = null;
		MxlDefaults defaults = null;
		PVector credits = PVector.pvec();
		MxlPartList partList = null;
		for (Element c : XMLReader.elements(e)) {
			String n = c.getNodeName();
			switch (n.charAt(0)) {
			case 'c':
				if (!n.equals("credit"))
					break;
				credits = credits.plus(MxlCredit.read(c));
				break;
			case 'd':
				if (!n.equals("defaults"))
					break;
				defaults = MxlDefaults.read(c);
				break;
			case 'i':
				if (!n.equals("identification"))
					break;
				identification = MxlIdentification.read(c);
				break;
			case 'm':
				if (n.equals("movement-number")) {
					movementNumber = c.getTextContent();
				} else {
					if (!n.equals("movement-title"))
						break;
					movementTitle = c.getTextContent();
				}
				break;
			case 'p':
				if (!n.equals("part-list"))
					break;
				partList = MxlPartList.read(c);
				break;
			case 'w':
				if (!n.equals("work"))
					break;
				work = MxlWork.read(c);
			}
		}

		return new MxlScoreHeader(work, movementNumber, movementTitle, identification, defaults,
				credits, InvalidMusicXML.throwNull(partList, e));
	}

	public void write(Element e) {
		if (this.work != null)
			this.work.write(e);
		XMLWriter.addElement("movement-number", this.movementNumber, e);
		XMLWriter.addElement("movement-title", this.movementTitle, e);
		if (this.identification != null)
			this.identification.write(e);
		if (this.defaults != null)
			this.defaults.write(e);
		for (MxlCredit credit : this.credits)
			credit.write(e);
		this.partList.write(e);
	}

	public void paint(PaintTransfer pt) {
		this.getDefaults().paint(pt);
		this.getPartList().paint(pt);
		for (MxlCredit credit : this.credits)
			credit.paint(pt);
	}
}