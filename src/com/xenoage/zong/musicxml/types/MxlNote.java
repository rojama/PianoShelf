package com.xenoage.zong.musicxml.types;

import java.util.List;
import java.util.Vector;

import org.w3c.dom.Element;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;

import com.rojama.pianoshelf.DebugLog;
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
		// ===== 修复：全量收集阶段不过早 return，否则非当前页音符不会写入 scorePartsNotes =====
		// collectAllNotesForPlayback=true：扫描所有页，仅收集 note（不绘制到 canvas）
		// collectAllNotesForPlayback=false：按原逻辑，只处理当前页
		final boolean collectMode = pt.ct.collectAllNotesForPlayback;
		if (!collectMode && pt.nowPage != pt.ct.getDisPageNo())
			return;

		pt.measureUp = pt.getMeasureUp(getStaff());

		//定义音符用于演奏
		Note note = new Note();
		note.mxlNote = this;
		note.measureNum=pt.nowMeasure;		
		note.pageNum = pt.nowPage;
		note.partID=pt.nowPartID;
		// ====== 定位 bug-1 修复前置准备：
		// note.point 与 addNote 延后到"Pitch/Rest 坐标完全算完之后"再执行（否则 oldY=0/脏值）。
		// 这里先用两个变量暂存。
		boolean pendingNote = false;       // 标记"该音符需要登记到 scorePartsNotes"
		float pendingNoteRelX = pt.oldX;   // 在 measure 内的相对 X（chord 不前进，用此时 oldX）
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
			// ===== 修复 tick 对齐：直接存 divisions 单位的 nowDuration，不再做 *64/divisions
			// 真正的播放 tick 换算放到 buildPlaybackPlan 中统一处理 =====
			note.duration = pt.nowDuration;
			note.divisions = Math.max(1, pt.divisions);
			
			//加入音符（音高先登记；坐标/正式入库延后到绘制分支里）
			if (mnn.getFullNote().getContent().getFullNoteContentType() == MxlFullNoteContentType.Pitch){
				note.pitch = ((MxlPitch) mnn.getFullNote().getContent()).getPitch();				
			}
			pendingNote = true;  // Pitch/Rest 都走 pendingNote 流程
			if (!mnn.getFullNote().isChord()){
				pt.nowDuration += mnn.getDuration();
			}
		}
		
		float oldLastNoteX = pt.lastNoteX;
		
		// 取音符坐标（printStyle default-x/y / relative-x/y 会改 oldX、oldY）
		if (this.getPrintStyle() != null) {
			pt.setPointInMeasure(this.getPrintStyle().getPosition());
			// printStyle 改了 X → 同步暂存
			pendingNoteRelX = pt.oldX;
//			if (pt.oldX <= pt.lastNoteX + pt.ct.NOTE_WIDTH && pt.oldX >= pt.lastNoteX - pt.ct.NOTE_WIDTH) {
//				isSameX = true;
//			}
			// 记录上一个音符的X坐标
			if (!isSameX) {
				pt.lastNoteX = pt.oldX;
			}
		}
		
		// System.out.println(isSameX + "|" + pt.oldX + "|" + pt.lastNoteX);

		// ====== 1) 先决定 staff 编号（默认 1，为 null 时兜底） ======
		Integer staffNum = this.getStaff();
		int staff = (staffNum == null) ? 1 : staffNum.intValue();

		// ====== 2) 若这个 staff 没有谱号，默认给一个 G（nowClefType 为空是首个 measure 还没遇到 attributes/clef 时最常见的崩点） ======
		if (!pt.nowClefType.containsKey(Integer.valueOf(staff))) {
			DebugLog.w("MxlNote", "  staff=" + staff + " 无 clef 记录，兜底 ClefType.G（nowClefType.keys=" + pt.nowClefType.keySet() + "）");
			pt.nowClefType.put(Integer.valueOf(staff), com.xenoage.zong.core.music.clef.ClefType.G);
		}

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
				default:
					symbol = pt.ct.symbolPool.getSymbol(CommonSymbol.NoteQuarter);
					break;
				}
				if (symbol == null) {
					DebugLog.w("MxlNote", "  note head symbol 为 null，跳过此 note（type=" + this.getType().type + "）");
					break;
				}
				MxlPitch pitch = (MxlPitch) this.getContent().getFullNote().getContent();
				com.xenoage.zong.core.music.clef.ClefType ct = pt.nowClefType.get(Integer.valueOf(staff));
				if (ct == null || pitch == null || pitch.getPitch() == null) {
					DebugLog.w("MxlNote", "  clef/pitch 缺一项，跳过绘制: clef=" + ct + " pitchObj=" + pitch + " getPitch()=" + (pitch == null ? "null" : pitch.getPitch()));
					break;
				}
				int line = ct.computeLinePosition(pitch.getPitch());
				final int SP = pt.ct.STAFF_LINE_SPACING;
				pt.oldY = 4 * SP - line * SP / 2;

				// ====== 定位 bug-1 修复:
				// 只有在 oldY 正确计算后（考虑 clef + line + printStyle），
				// 才能把 note 坐标写入 Note.point，并存入 scorePartsNotes 供高亮/播放索引。
				// Note.point 存绝对画布坐标：(measureLeft + relX, measureUp + oldY)
				if (pendingNote) {
					note.point = new PointF(pt.measureLeft + pendingNoteRelX,
							pt.measureUp + pt.oldY);
					if (!pt.ct.scorePartsNotes.containsKey(pt.nowPartID)) {
						pt.ct.scorePartsNotes.put(pt.nowPartID, new Vector<Note>());
					}
					pt.ct.scorePartsNotes.get(pt.nowPartID).add(note);
					pendingNote = false;
				}
				// note head 绘制：topToBase 按 SYMBOL_SCALE 同比缩
				pt.drawBitmap(symbol.getBitmap(), pt.measureLeft + pt.oldX, pt.measureUp + pt.oldY
						- pt.symTopToBase(symbol) + 1);
				final int noteSymW = pt.symW(symbol);
				
				// 加线：上下超出 staff 的音符补短横线
				if (line < 0) {
					for (int i = -2; i >= line; i = i - 2) {
						float y = pt.measureUp + (-i / 2 + 4) * SP;
						pt.drawLine(pt.measureLeft + pt.oldX - 5, y, pt.measureLeft + pt.oldX
								+ noteSymW + 5, y);
					}
				} else if (line > 8) {
					for (int i = 10; i <= line; i = i + 2) {
						float y = pt.measureUp - (i / 2 - 4) * SP;
						pt.drawLine(pt.measureLeft + pt.oldX - 5, y, pt.measureLeft + pt.oldX
								+ noteSymW + 5, y);
					}
				}

				// 画辅助符号（容错 try：单个 notation 出错不影响整段）
				try {
					for (MxlNotations notaion : this.getNotations()) {
						for (MxlNotationsContent content : notaion.getElements()) {
							switch (content.getNotationsContentType()) {
							case CurvedLine:
								PointF end = new PointF(pt.measureLeft + noteSymW
										/ 2f + pt.oldX, pt.measureUp + pt.oldY);
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
												startM.y += (stemValue == MxlStemValue.Down)? -1*Math.round(SP*1.5f) : Math.round(SP*1.5f);
												pt.drawDefaultBezier(start, startM, stemValue);
												
												PointF endM = new PointF();
												endM.set(end);
												endM.x = pt.getMxlAllMargins().getLeftMargin();
												endM.y += (stemValue == MxlStemValue.Down)? -1*Math.round(SP*1.5f) : Math.round(SP*1.5f);
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
				} catch (Throwable t) {
					DebugLog.w("MxlNote", "  画 notations 出错（忽略，不影响后续音符）", t);
				}

				// 画符干（WHOLE 音符没有符干）
				// 【修复】：stem X/Y 与 SYMBOL_SCALE/STAFF_LINE_SPACING 强一致：
				//   Up 情况：note head 右侧 (left + noteSymW - 1)，startY = head 顶部 + 半个 line(SP/2)，
				//           stopY = startY - NOTE_LINE_HIGHT（标准 3.5 SP）
				//   Down 情况：note head 左侧 + 1，startY = head 底部 - SP/2
				//             stopY = startY + NOTE_LINE_HIGHT
				// 同时处理 chord（isSameX）时左右 note stem 的对齐。
				if (this.getType().type.ordinal() < MxlTypeValue.WHOLE.ordinal()) {
					final int TTBASE = pt.symTopToBase(symbol);  // note head 的 top→第4线(base) 距离
					// head 在 canvas 上的 top Y:
					final float headTopY = pt.measureUp + pt.oldY - TTBASE + 1;
					// head 的 base Y = topY + note head 可视高 (≈ symH(symbol) 为总高；base 对应 TTBASE)
					// note head 在标准中是 1 SP 高，我们使用 SP 计算 baseY 来对齐，避免不同缩放时 stem 错位：
					final float headBaseY = headTopY + TTBASE;   // 五线谱 4 线线位置(=最底 staff line +1/2SP 对应 note 的 base)
					// 最常用：stem 起点 Y 对齐 note head 的 上/下缘（相对 pitch line）
					float beginX = pt.measureLeft + pt.oldX;
					float startY, stopY;
					// 根据 stemValue 选 startY 和 beginX
					switch (stemValue) {
					case Up:
						// stem 从 note head 右上出发 (Up 时 stem 在 head 右边，从 head TOP+SP/2 向上)
						if (isSameX && pt.oldX > oldLastNoteX) {
							// chord: 下方 note X 偏右，stem 画在左边（和上一个对齐）
						} else {
							beginX += noteSymW - 1;
						}
						// startY = head baseY (head 下边缘处) → 向上减 NOTE_LINE_HIGHT 得到 top 端点
						startY = headBaseY - SP / 2f;
						// stopY = startY - NOTE_LINE_HIGHT，但最短到 headTopY 以上 3 SP
						stopY = startY - pt.ct.NOTE_LINE_HIGHT;
						break;
					case Down:
					default:
						// stem 从 note head 左下出发 (Down 时 stem 在 head 左边)
						if (isSameX && pt.oldX < oldLastNoteX) {
							beginX += noteSymW - 1;
						} else {
							// beginX 不变 (最左 edge)
						}
						startY = headTopY + SP / 2f;
						stopY = startY + pt.ct.NOTE_LINE_HIGHT;
						break;
					}

					if (stemPosition.getDefaultY() != null) {
						float sc = (pt.ct.tenthsToPx > 0f) ? pt.ct.tenthsToPx : 1f;
						// XML stem@default-y 单位 tenths，取相对 measureUp，向下为正
						stopY = pt.measureUp - stemPosition.getDefaultY() * sc;
					}

					pt.drawLine(beginX, startY, beginX, stopY);

					if (this.getBeams().size() > 0) {
						// 画符梁：每条 beam 偏移一个 STAFF_LINE_SPACING，按 stem 方向相对 stopY 偏移
						float beamX = beginX, beamY = stopY;
						try {
							for (MxlBeam beam : this.getBeams()) {
								switch (stemValue) {
								case Up:
									beamY += (beam.getNumber() - 1) * SP;
									break;
								case Down:
									beamY -= (beam.getNumber() - 1) * SP;
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
											// 画粗点：3px 粗的线，也按 scale 放大
											int thick = Math.max(1, Math.round(SP * 0.35f));
											int half = (thick - 1) / 2;
											for (int i = -half; i <= half; i++) {
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
						} catch (Throwable t) {
							DebugLog.w("MxlNote", "  画符梁出错（忽略）", t);
						}
					} else if (pt.lastBeamPoint.isEmpty() && !isSameX) {
						// 画符旗 (flags)
						Symbol flagSym = pt.ct.symbolPool.getSymbol(CommonSymbol.NoteFlag);
						if (flagSym != null && flagSym.getBitmap() != null) {
							final int flagH = pt.symH(flagSym);
							final int flagTTB = pt.symTopToBase(flagSym);
							float flagHight = Math.max(SP, flagH - flagTTB);
							int flags = MxlTypeValue.QUARTER.ordinal() - this.getType().type.ordinal();
							for (int i = 0; i < flags; i++) {
								switch (stemValue) {
								case Up:
									// flag 挂在 stem 末端 (stopY) ，向下叠
									pt.drawBitmap(flagSym.getBitmap(), beginX, stopY + i * flagHight);
									break;
								case Down:
									// Down 方向：先镜像，再对齐 stem 末端
									Matrix mx = new Matrix();
									mx.setScale(1, -1);
									Bitmap flagOrigBitmap = flagSym.getBitmap();
									Bitmap newBitmap = Bitmap.createBitmap(flagOrigBitmap, 0, 0,
											flagOrigBitmap.getWidth(), flagOrigBitmap.getHeight(), mx, true);
									// flag 高 = flagHight → 第 i 个从 stopY 向上偏移 (i+1)*flagHight
									pt.drawBitmap(newBitmap, beginX, stopY - (i + 1) * flagHight);
									break;
								}
							}
						}
					}
				}

				pt.oldX += noteSymW + pt.ct.SPACE;
				break;

			// 休止符
			case Rest:
				final int SP_R = pt.ct.STAFF_LINE_SPACING;
				pt.oldY = 2 * SP_R;
				// ====== 定位 bug-1 修复：休止符也要在算完 oldY 后写入 scorePartsNotes（方便后续高亮/光标指示）
				if (pendingNote) {
					note.point = new PointF(pt.measureLeft + pendingNoteRelX,
							pt.measureUp + pt.oldY);
					if (!pt.ct.scorePartsNotes.containsKey(pt.nowPartID)) {
						pt.ct.scorePartsNotes.put(pt.nowPartID, new Vector<Note>());
					}
					pt.ct.scorePartsNotes.get(pt.nowPartID).add(note);
					pendingNote = false;
				}
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
					if (symbol != null) {
						pt.drawBitmap(symbol.getBitmap(), pt.measureLeft + pt.oldX, pt.measureUp
								+ pt.oldY - pt.symTopToBase(symbol));
						pt.oldX += pt.symW(symbol) + pt.ct.SPACE;
					}
					symbol = pt.ct.symbolPool.getSymbol(CommonSymbol.RestHalf);
					break;
				case LONG:
					symbol = pt.ct.symbolPool.getSymbol(CommonSymbol.RestWhole);
					if (symbol != null) {
						pt.drawBitmap(symbol.getBitmap(), pt.measureLeft + pt.oldX, pt.measureUp
								+ pt.oldY - pt.symTopToBase(symbol));
						pt.oldX += pt.symW(symbol) + pt.ct.SPACE;
					}
					break;
				}
				if (symbol != null) {
					pt.drawBitmap(symbol.getBitmap(), pt.measureLeft + pt.oldX, pt.measureUp + pt.oldY
							- pt.symTopToBase(symbol));
					pt.oldX += pt.symW(symbol) + pt.ct.SPACE;
				}
				break;
			}
		}

		// 画附点符
		{
			final int SP = pt.ct.STAFF_LINE_SPACING;
			// ====== 定位 bug-2 修复：
			// 原代码 if (pt.oldY % SP == 0) pt.oldY -= SP/2 会修改全局 pt.oldY，
			// 导致后续判断"下一个音符/附点"的 Y 坐标被意外上移半个间距。
			// 改用局部变量 dotY，只在绘制附点时应用临时调整，绝不污染全局 pt.oldY。
			float dotY = pt.oldY;
			for (int i = 0; i < this.getDot(); i++) {
				float curDotY = dotY;
				if (curDotY % SP == 0) { // 防止附点符被五线谱覆盖（当线与附点Y对齐时上移半个间距）
					curDotY -= SP / 2f;
				}
				Symbol dotSymbol = pt.ct.symbolPool.getSymbol(CommonSymbol.NoteDot);
				if (dotSymbol != null) {
					pt.drawBitmap(dotSymbol.getBitmap(), pt.measureLeft + pt.oldX, pt.measureUp + curDotY
							- pt.symTopToBase(dotSymbol));
					pt.oldX += pt.symW(dotSymbol) + pt.ct.SPACE;
				}
			}
		}

		// 画歌词：与 staff 距离按 STAFF_LINE_SPACING 同比
		{
			final int SP = pt.ct.STAFF_LINE_SPACING;
			float lyricY = pt.measureUp + 4 * SP + Math.round(SP * 4.4f); // 原 + 80 (≈10*8) 缩为 SP*8.4
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
				lyricY += SP; // 每行歌词间隔 = 1 个 line spacing
			}
		}

	}

}