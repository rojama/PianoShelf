package com.xenoage.zong.musicxml.types;

import java.util.List;
import java.util.Vector;

import org.w3c.dom.Element;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;

import com.rojama.pianoshelf.Note;
import com.rojama.pianoshelf.PaintTransfer;
import com.xenoage.pdlib.PVector;
import com.xenoage.util.annotations.MaybeEmpty;
import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.MxlType.MxlTypeValue;
import com.xenoage.zong.musicxml.types.attributes.MxlPosition;
import com.xenoage.zong.musicxml.types.attributes.MxlPrintStyle;
import com.xenoage.zong.musicxml.types.choice.MxlMusicDataContent;
import com.xenoage.zong.musicxml.types.choice.MxlNotationsContent;
import com.xenoage.zong.musicxml.types.choice.MxlNoteContent;
import com.xenoage.zong.musicxml.types.choice.MxlFullNoteContent.MxlFullNoteContentType;
import com.xenoage.zong.musicxml.types.choice.MxlNoteContent.MxlNoteContentType;
import com.xenoage.zong.musicxml.types.enums.MxlBeamValue;
import com.xenoage.zong.musicxml.types.enums.MxlStartStopContinue;
import com.xenoage.zong.musicxml.types.enums.MxlStemValue;
import com.xenoage.zong.musicxml.types.groups.MxlEditorialVoice;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import com.xenoage.zong.musicxml.util.Parse;
import com.xenoage.zong.symbols.Symbol;
import com.xenoage.zong.symbols.common.CommonSymbol;

@IncompleteMusicXML(missing = "accidental,time-modification,notehead,x-position,font,color,printout,dynamics,end-dynamics,attack,release,time-only,pizzicato", children = "beam,editorial-voice,notations,lyric")
public final class MxlNote implements MxlMusicDataContent {
	public static final String ELEM_NAME = "note";

	@NeverNull
	private final MxlNoteContent content;

	@MaybeNull
	private MxlType type;

	@NeverNull
	private final int dot; // 附点数目
	
	@NeverNull
	private final Integer duration; 

	@MaybeNull
	private final MxlPrintStyle printStyle;

	@MaybeNull
	private final MxlInstrument instrument;

	@NeverNull
	private final MxlEditorialVoice editorialVoice;

	@MaybeNull
	private final MxlStem stem;

	@MaybeNull
	private final Integer staff;

	@MaybeEmpty
	private final PVector<MxlBeam> beams;

	@MaybeEmpty
	private final PVector<MxlNotations> notations;

	@MaybeEmpty
	private final PVector<MxlLyric> lyrics;
	private static final PVector<MxlBeam> beamsEmpty = PVector.pvec();
	private static final PVector<MxlNotations> notationsEmpty = PVector.pvec();
	private static final PVector<MxlLyric> lyricsEmpty = PVector.pvec();

	public MxlNote(MxlNoteContent content, MxlType type, int dot, Integer duration, MxlInstrument instrument,
			MxlEditorialVoice editorialVoice, MxlStem stem, Integer staff, PVector<MxlBeam> beams,
			PVector<MxlNotations> notations, PVector<MxlLyric> lyrics, MxlPrintStyle mxlPrintStyle) {
		this.type = type;
		this.dot = dot;
		this.duration = duration;
		this.content = content;
		this.instrument = instrument;
		this.editorialVoice = editorialVoice;
		this.stem = stem;
		this.staff = staff;
		this.beams = beams;
		this.notations = notations;
		this.lyrics = lyrics;
		this.printStyle = mxlPrintStyle;
	}

	@NeverNull
	public MxlNoteContent getContent() {
		return this.content;
	}

	@MaybeNull
	public MxlType getType() {
		return this.type;
	}

	@NeverNull
	public int getDot() {
		return this.dot;
	}

	@MaybeNull
	public MxlPrintStyle getPrintStyle() {
		return this.printStyle;
	}

	@MaybeNull
	public MxlInstrument getInstrument() {
		return this.instrument;
	}

	@NeverNull
	public MxlEditorialVoice getEditorialVoice() {
		return this.editorialVoice;
	}

	@MaybeNull
	public MxlStem getStem() {
		return this.stem;
	}

	@MaybeNull
	public Integer getStaff() {
		return this.staff;
	}

	@MaybeEmpty
	public PVector<MxlBeam> getBeams() {
		return this.beams;
	}

	@MaybeEmpty
	public PVector<MxlNotations> getNotations() {
		return this.notations;
	}

	@MaybeEmpty
	public PVector<MxlLyric> getLyrics() {
		return this.lyrics;
	}

	@Override
	public MxlMusicDataContent.MxlMusicDataContentType getMusicDataContentType() {
		return MxlMusicDataContent.MxlMusicDataContentType.Note;
	}

	@NeverNull
	public static MxlNote read(Element e) {
		MxlNoteContent content = readNoteContent(e);
		MxlInstrument instrument = null;
		MxlEditorialVoice editorialVoice = MxlEditorialVoice.read(e);
		List<Element> children = XMLReader.elements(e);
		MxlStem stem = null;
		MxlType type = null;
		Integer staff = 1;
		Integer duration = 1;
		int dot = 0;
		PVector<MxlBeam> beams = beamsEmpty;
		PVector<MxlNotations> notations = notationsEmpty;
		PVector<MxlLyric> lyrics = lyricsEmpty;
		for (Element child : children) {
			String n = child.getNodeName();
			switch (n.charAt(0)) {
			case 's':
				if (n.equals("stem")) {
					stem = MxlStem.read(child);
				} else {
					if (!n.equals("staff"))
						break;
					staff = Integer.valueOf(Parse.parseInt(child));
				}
				break;
			case 't':
				if (!n.equals("type"))
					break;
				type = MxlType.read(child);
				break;
			case 'd':
				if (n.equals("dot")){					
					dot++;
				} else {
						if (!n.equals("duration"))
							break;
						duration = Integer.valueOf(Parse.parseInt(child));
					}
				break;
			case 'b':
				if (!n.equals("beam"))
					break;
				beams = beams.plus(MxlBeam.read(child));
				break;
			case 'i':
				if (!n.equals("instrument"))
					break;
				instrument = MxlInstrument.read(child);
				break;
			case 'n':
				if (!n.equals("notations"))
					break;
				notations = notations.plus(MxlNotations.read(child));
				break;
			case 'l':
				if (!n.equals("lyric"))
					break;
				lyrics = lyrics.plus(MxlLyric.read(child));
			}
		}

		return new MxlNote(content, type, dot, duration, instrument, editorialVoice, stem, staff, beams,
				notations, lyrics, MxlPrintStyle.read(e));
	}

	@Override
	public void write(Element parent) {
		Element e = XMLWriter.addElement("note", parent);
		this.content.write(e);
		if (this.instrument != null)
			this.instrument.write(e);
		this.editorialVoice.write(e);
		if (this.stem != null)
			this.stem.write(e);
		XMLWriter.addElement("staff", this.staff, e);
		for (MxlBeam beam : this.beams)
			beam.write(e);
		for (MxlNotations n : this.notations)
			n.write(e);
		for (MxlLyric lyric : this.lyrics)
			lyric.write(e);
	}

	private static MxlNoteContent readNoteContent(Element e) {
		Element firstChild = XMLReader.element(e);
		String n = firstChild.getNodeName();
		if (n.equals("grace"))
			return MxlGraceNote.read(e);
		if (n.equals("cue")) {
			return MxlCueNote.read(e);
		}
		return MxlNormalNote.read(e);
	}

	@Override
	public void print(PaintTransfer pt) {
		if (pt.nowPage != pt.ct.getDisPageNo())
			return;

		pt.measureUp = pt.getMeasureUp(getStaff());

		//定义音符用于演奏
		Note note = new Note();
		note.mxlNote = this;
		note.measureNum=pt.nowMeasure;		
		note.pageNum = pt.nowPage;
		note.partID=pt.nowPartID;
//		System.out.println(this.duration + " * 64 / " + pt.divisions);
		
		if (this.getType() == null && this.getContent() != null && pt.divisions != 0) {
			MxlNormalNote mnn = (MxlNormalNote) content;
			//System.out.println(mnn.getDuration() + " * 256 / " + pt.divisions);
			int timeLong = mnn.getDuration() * 64 / pt.divisions;		
			switch (timeLong) {
			case 1:
				this.type = new MxlType(MxlTypeValue._256TH, null);
				break;
			case 2:
				this.type = new MxlType(MxlTypeValue._128TH, null);
				break;
			case 4:
				this.type = new MxlType(MxlTypeValue._64TH, null);
				break;
			case 8:
				this.type = new MxlType(MxlTypeValue._32ND, null);
				break;
			case 16:
				this.type = new MxlType(MxlTypeValue._16TH, null);
				break;
			case 32:
				this.type = new MxlType(MxlTypeValue.EIGHTH, null);
				break;
			case 64:
				this.type = new MxlType(MxlTypeValue.QUARTER, null);
				break;
			case 128:
				this.type = new MxlType(MxlTypeValue.HALF, null);
				break;
			case 256:
				this.type = new MxlType(MxlTypeValue.WHOLE, null);
				break;
			case 512:
				this.type = new MxlType(MxlTypeValue.BREVE, null);
				break;
			case 1024:
				this.type = new MxlType(MxlTypeValue.LONG, null);
				break;
			}
		}

		// 是否和上面的音符在同一个X坐标
		boolean isSameX = false;
		
		if (this.getContent().getNoteContentType() == MxlNoteContentType.Normal) {
			MxlNormalNote mnn = (MxlNormalNote) content;
			isSameX = mnn.getFullNote().isChord();
			note.duration = pt.nowDuration * 64 / pt.divisions;
			
			//加入音符
			if (mnn.getFullNote().getContent().getFullNoteContentType() == MxlFullNoteContentType.Pitch){
				note.pitch = ((MxlPitch) mnn.getFullNote().getContent()).getPitch();				
			}
			note.point = new PointF(pt.oldX,pt.oldY);
			if (!pt.ct.scorePartsNotes.containsKey(pt.nowPartID)){
				pt.ct.scorePartsNotes.put(pt.nowPartID, new Vector<Note>());
			}
			
			pt.ct.scorePartsNotes.get(pt.nowPartID).add(note);
			if (!mnn.getFullNote().isChord()){
				pt.nowDuration += mnn.getDuration();
			}
			
//			if (isSameX){
//				Vector<Note> notes = pt.ct.scorePartsNotes.get(pt.nowPartID).lastElement();
//				notes.add(note);
//			}else{
//				Vector<Note> notes = new Vector<Note>();
//				notes.add(note);
//				
//			}
		}
		
		float oldLastNoteX = pt.lastNoteX;
		
		// 取音符坐标
		if (this.getPrintStyle() != null) {
			pt.setPointInMeasure(this.getPrintStyle().getPosition());
//			if (pt.oldX <= pt.lastNoteX + pt.ct.NOTE_WIDTH && pt.oldX >= pt.lastNoteX - pt.ct.NOTE_WIDTH) {
//				isSameX = true;
//			}
			// 记录上一个音符的X坐标
			if (!isSameX) {
				pt.lastNoteX = pt.oldX;
			}
		}
		
		// System.out.println(isSameX + "|" + pt.oldX + "|" + pt.lastNoteX);

		// 画音符
		if (this.getType() != null) {
			MxlStemValue stemValue = MxlStemValue.None;
			MxlPosition stemPosition = MxlPosition.empty;
			if (this.getStem() != null) {
				stemValue = this.getStem().getValue();
				stemPosition = this.getStem().getYPosition();
			}

			Symbol symbol = null;
			switch (this.getContent().getFullNote().getContent().getFullNoteContentType()) {
			// 音符
			case Pitch:
				switch (this.getType().type) {
				case _256TH:
				case _128TH:
				case _64TH:
				case _32ND:
				case _16TH:
				case EIGHTH:
				case QUARTER:
					symbol = pt.ct.symbolPool.getSymbol(CommonSymbol.NoteQuarter);
					break;
				case HALF:
					symbol = pt.ct.symbolPool.getSymbol(CommonSymbol.NoteHalf);
					break;
				case WHOLE:
					symbol = pt.ct.symbolPool.getSymbol(CommonSymbol.NoteWhole);
					break;
				}
				MxlPitch pitch = (MxlPitch) this.getContent().getFullNote().getContent();
				int line = pt.nowClefType.get(this.getStaff())
						.computeLinePosition(pitch.getPitch());
				pt.oldY = 4 * 10 - line * 10 / 2;
				pt.drawBitmap(symbol.getBitmap(), pt.measureLeft + pt.oldX, pt.measureUp + pt.oldY
						- symbol.getTopToBase() + 1);
				
				
				

				// 加线
				if (line < 0) {
					for (int i = -2; i >= line; i = i - 2) {
						float y = pt.measureUp + (-i / 2 + 4) * 10;
						pt.drawLine(pt.measureLeft + pt.oldX - 5, y, pt.measureLeft + pt.oldX
								+ symbol.getBitmap().getWidth() + 5, y);
					}
				} else if (line > 8) {
					for (int i = 10; i <= line; i = i + 2) {
						float y = pt.measureUp - (i / 2 - 4) * 10;
						pt.drawLine(pt.measureLeft + pt.oldX - 5, y, pt.measureLeft + pt.oldX
								+ symbol.getBitmap().getWidth() + 5, y);
					}
				}

				// 画辅助符号
				for (MxlNotations notaion : this.getNotations()) {
					for (MxlNotationsContent content : notaion.getElements()) {
						switch (content.getNotationsContentType()) {
						case CurvedLine:
							PointF end = new PointF(pt.measureLeft + symbol.getBitmap().getWidth()
									/ 2 + pt.oldX, pt.measureUp + pt.oldY);
							MxlCurvedLine curvedLine = (MxlCurvedLine) content;
							for (MxlCurvedLine lastCurvedLine : pt.lastCurvedLinePoint.keySet()) {
								if (lastCurvedLine.getNumber() == curvedLine.getNumber()) {
									switch (curvedLine.getType()) {
									case Start:
										pt.lastCurvedLinePoint.remove(lastCurvedLine);
										break;
									case Continue:
									case Stop:
										PointF start = pt.lastCurvedLinePoint.get(lastCurvedLine);
										if (start.x <= end.x){
											pt.drawDefaultBezier(start, end, stemValue);
										}else{
											PointF startM = new PointF();
											startM.set(start);
											startM.x = pt.getPageWidth()-pt.getMxlAllMargins().getRightMargin();
											startM.y += (stemValue == MxlStemValue.Down)? -15:15;												
											pt.drawDefaultBezier(start, startM, stemValue);
											
											PointF endM = new PointF();
											endM.set(end);
											endM.x = pt.getMxlAllMargins().getLeftMargin();
											endM.y += (stemValue == MxlStemValue.Down)? -15:15;
											pt.drawDefaultBezier(endM, end, stemValue);
										}
										pt.lastCurvedLinePoint.remove(lastCurvedLine);
										break;
									}
									break;
								}
							}
							if (!pt.lastCurvedLinePoint.containsKey(curvedLine)
									&& curvedLine.getType() != MxlStartStopContinue.Stop) {
								pt.lastCurvedLinePoint.put(curvedLine, end);
							}
						case Dynamics:
						case Articulations:
							// TODO
						}
					}
				}

				// 画符干
				if (this.getType().type.ordinal() < MxlTypeValue.WHOLE.ordinal()) {
					float beginX = pt.measureLeft + pt.oldX, startY = pt.measureUp + pt.oldY, stopY = startY;

					switch (stemValue) {
					case Up:
						if (isSameX && pt.oldX > oldLastNoteX) {
							// beginX -= 1;
						} else {
							beginX += symbol.getBitmap().getWidth() - 1;
						}
						stopY -= pt.ct.NOTE_LINE_HIGHT;
						break;
					case Down:
						if (isSameX && pt.oldX < oldLastNoteX) {
							beginX += symbol.getBitmap().getWidth() - 1;
						} else {
							// beginX += 1;
						}
						stopY += pt.ct.NOTE_LINE_HIGHT;
						break;
					}

					if (stemPosition.getDefaultY() != null) {
						stopY = pt.measureUp - stemPosition.getDefaultY();
					}

					pt.drawLine(beginX, startY, beginX, stopY);

					if (this.getBeams().size() > 0) {
						// 画符梁
						float beamX = beginX, beamY = stopY;
						for (MxlBeam beam : this.getBeams()) {
							switch (stemValue) {
							case Up:
								beamY += (beam.getNumber() - 1) * 10;
								break;
							case Down:
								beamY -= (beam.getNumber() - 1) * 10;
								break;
							}
							for (MxlBeam lastBeam : pt.lastBeamPoint.keySet()) {
								if (lastBeam.hashCode() == beam.hashCode()) {
									switch (beam.getValue()) {
									case Begin:
										pt.lastBeamPoint.remove(lastBeam);
										break;
									case Continue:
									case End:
										PointF point = pt.lastBeamPoint.get(lastBeam);
										// 画粗点
										for (int i = -1; i <= 1; i++) {
											pt.drawLine(point.x, point.y + i, beamX, beamY + i);
										}
										pt.lastBeamPoint.remove(lastBeam);
										break;
									}
									break;
								}
							}
							if (!pt.lastBeamPoint.containsKey(beam)
									&& beam.getValue() != MxlBeamValue.End) {
								pt.lastBeamPoint.put(beam, new PointF(beamX, beamY));
							}
						}
					} else if (pt.lastBeamPoint.isEmpty() && !isSameX) {
						// 画符旗
						symbol = pt.ct.symbolPool.getSymbol(CommonSymbol.NoteFlag);
						for (int i = 0; i < MxlTypeValue.QUARTER.ordinal()
								- this.getType().type.ordinal(); i++) {
							float flagHight = symbol.getBitmap().getHeight()
									- symbol.getTopToBase();
							switch (stemValue) {
							case Up:
								pt.drawBitmap(symbol.getBitmap(), beginX, stopY + i * flagHight);
								break;
							case Down:
								Matrix mx = new Matrix();
								mx.setScale(1, -1); // 产生镜像
								Bitmap newBitmap = Bitmap.createBitmap(symbol.getBitmap(), 0, 0,
										symbol.getBitmap().getWidth(), symbol.getBitmap()
												.getHeight(), mx, true);

								pt.drawBitmap(newBitmap, beginX, stopY - (i + 2) * flagHight);
								break;
							}
						}
					}
				}

				pt.oldX += symbol.getBitmap().getWidth() + pt.ct.SPACE;
				break;

			// 休止符
			case Rest:
				pt.oldY = 2 * 10;
				switch (this.getType().type) {
				case _256TH:
					symbol = pt.ct.symbolPool.getSymbol(CommonSymbol.Rest256th);
					break;
				case _128TH:
					symbol = pt.ct.symbolPool.getSymbol(CommonSymbol.Rest128th);
					break;
				case _64TH:
					symbol = pt.ct.symbolPool.getSymbol(CommonSymbol.Rest64th);
					break;
				case _32ND:
					symbol = pt.ct.symbolPool.getSymbol(CommonSymbol.Rest32th);
					break;
				case _16TH:
					symbol = pt.ct.symbolPool.getSymbol(CommonSymbol.Rest16th);
					break;
				case EIGHTH:
					symbol = pt.ct.symbolPool.getSymbol(CommonSymbol.RestEighth);
					break;
				case QUARTER:
					symbol = pt.ct.symbolPool.getSymbol(CommonSymbol.RestQuarter);
					break;
				case HALF:
					symbol = pt.ct.symbolPool.getSymbol(CommonSymbol.RestHalf);
					break;
				case WHOLE:
					symbol = pt.ct.symbolPool.getSymbol(CommonSymbol.RestWhole);
					break;
				case BREVE:
					symbol = pt.ct.symbolPool.getSymbol(CommonSymbol.RestWhole);
					pt.drawBitmap(symbol.getBitmap(), pt.measureLeft + pt.oldX, pt.measureUp
							+ pt.oldY - symbol.getTopToBase());
					pt.oldX += symbol.getBitmap().getWidth() + pt.ct.SPACE;
					symbol = pt.ct.symbolPool.getSymbol(CommonSymbol.RestHalf);
					break;
				case LONG:
					symbol = pt.ct.symbolPool.getSymbol(CommonSymbol.RestWhole);
					pt.drawBitmap(symbol.getBitmap(), pt.measureLeft + pt.oldX, pt.measureUp
							+ pt.oldY - symbol.getTopToBase());
					pt.oldX += symbol.getBitmap().getWidth() + pt.ct.SPACE;
					break;
				}
				pt.drawBitmap(symbol.getBitmap(), pt.measureLeft + pt.oldX, pt.measureUp + pt.oldY
						- symbol.getTopToBase());
				pt.oldX += symbol.getBitmap().getWidth() + pt.ct.SPACE;
				break;
			}
		}

		// 画附点符
		for (int i = 0; i < this.getDot(); i++) {
			if (pt.oldY % 10 == 0) { // 防止附点符被五线谱覆盖
				pt.oldY -= 10 / 2;
			}
			Symbol dotSymbol = pt.ct.symbolPool.getSymbol(CommonSymbol.NoteDot);
			pt.drawBitmap(dotSymbol.getBitmap(), pt.measureLeft + pt.oldX, pt.measureUp + pt.oldY
					- dotSymbol.getTopToBase());
			pt.oldX += dotSymbol.getBitmap().getWidth() + pt.ct.SPACE;
		}

		// 画歌词
		float lyricY = pt.measureUp + 80;
		for (MxlLyric lyric : this.getLyrics()) {
			pt.setPointInMeasure(lyric.getPosition());
			if (lyric.getPosition().getDefaultY() != null
					|| lyric.getPosition().getRelativeY() != null) {
				lyricY = pt.measureUp + pt.oldY;
			}
			switch (lyric.getContent().getLyricContentType()) {
			case SyllabicText:
				MxlSyllabicText st = (MxlSyllabicText) lyric.getContent();
				switch (st.getSyllabic()) {
				case Single:
					pt.drawText(st.getText().getValue(), pt.measureLeft + pt.oldX, lyricY);
				}
				break;
			case Extend:
				// TODO
			}
			lyricY += 10;
		}

	}

}