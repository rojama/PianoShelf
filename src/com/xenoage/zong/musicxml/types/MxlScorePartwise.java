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
	  com.rojama.pianoshelf.DebugLog.ensureInitialized(ct.context);
	  long t0 = System.currentTimeMillis();
	  com.rojama.pianoshelf.DebugLog.i("MxlPaint", "MxlScorePartwise.paint(ct) 开始：disPageNo=" + ct.disPageNo
			  + "  parts=" + (parts == null ? 0 : parts.size()) + "  maxPage(已知)=" + ct.maxPage
			  + "  collectAllNotesForPlayback=" + ct.collectAllNotesForPlayback);
	  // ===== 修复：全量收集阶段不清空 scorePartsNotes =====
	  // collectAllNotesForPlayback=true 时：从头扫描整份乐谱，把所有页所有音符累积到 scorePartsNotes
	  // collectAllNotesForPlayback=false 时：正常渲染，若之前已经跑过 collect 阶段则不清空（直接复用）
	  //   但 oldPaintTransfer/oldPartID 仍然要清空（渲染状态不能串）
	  if (!ct.collectAllNotesForPlayback && ct.scorePartsNotes.isEmpty()) {
		  // 只有"没做过 collect 阶段"的首次渲染才清空（兼容旧直接渲染路径）
		  ct.scorePartsNotes.clear();
	  }
	  ct.oldPaintTransfer.clear();
	  ct.oldPartID = null;
	  
	  PaintTransfer pt = new PaintTransfer();
	  pt.ct = ct;

	  com.rojama.pianoshelf.DebugLog.d("MxlPaint", "  → getScoreHeader().paint(pt) 绘制页眉/页脚/全局样式…");
	  long t1 = System.currentTimeMillis();
	  this.getScoreHeader().paint(pt);
	  long t2 = System.currentTimeMillis();
	  com.rojama.pianoshelf.DebugLog.i("MxlPaint", "     scoreHeader.paint 耗时=" + (t2 - t1) + "ms");

	  int iter = 0;
	  boolean run = true;
	  while (run){
		  iter++;
		  //ct.oldPartID = null;
		  long loopStart = System.currentTimeMillis();
		  int partIdx = 0;
		  for (MxlPart part : this.parts){
			  run = part.print(ct);
			  partIdx++;
			  //ct.oldPartID = part.getID();
		  }
		  long loopEnd = System.currentTimeMillis();
		  com.rojama.pianoshelf.DebugLog.d("MxlPaint", "    iter=" + iter
				  + "  part.print 轮次 parts=" + partIdx + " 耗时=" + (loopEnd - loopStart)
				  + "ms  继续下次循环?=" + run);
		  if (iter > 2000) {
			  com.rojama.pianoshelf.DebugLog.w("MxlPaint", "part.print 循环超过 2000 轮，强制 break（可能是 maxPage 计算 bug）");
			  break;
		  }
	  }

	  //打印页码
	  ct.paint.setTextSize(10);
//	  ct.drawText(ct.getDisPageNo()+"/"+ct.nowPage, ct.getPageWidth()-50, 50);
	  long tn = System.currentTimeMillis();
	  com.rojama.pianoshelf.DebugLog.i("MxlPaint", "MxlScorePartwise.paint 完成  总耗时=" + (tn - t0)
			  + "ms  最终 maxPage=" + ct.maxPage
			  + "  scorePartsNotes 收集了 " + ct.scorePartsNotes.size() + " 个声部"
			  + "  scoreParts.size=" + ct.scoreParts.size()
			  + "  最终 bitmap 宽高=" + (ct.bitmap == null ? "null" : (ct.bitmap.getWidth() + "x" + ct.bitmap.getHeight())));
  }
}