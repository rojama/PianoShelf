package com.xenoage.zong.musicxml.types;

import android.graphics.Paint;
import android.graphics.Point;

import com.rojama.pianoshelf.PaintTransfer;
import com.rojama.pianoshelf.PaintUtil;
import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.zong.musicxml.types.attributes.MxlPosition;
import com.xenoage.zong.musicxml.types.attributes.MxlPrintStyle;
import com.xenoage.zong.musicxml.types.enums.MxlLeftCenterRight;
import com.xenoage.zong.musicxml.types.enums.MxlVAlign;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="text-decoration,text-rotation,letter-spacing,line-height,xml:lang,text-direction,enclosure")
public final class MxlFormattedText
{

  @NeverNull
  private final String value;

  @MaybeNull
  private final MxlLeftCenterRight justify;

  @MaybeNull
  private final MxlLeftCenterRight hAlign;

  @MaybeNull
  private final MxlVAlign vAlign;

  @NeverNull
  private final MxlPrintStyle printStyle;

  public MxlFormattedText(String value, MxlLeftCenterRight justify, MxlLeftCenterRight hAlign, MxlVAlign vAlign, MxlPrintStyle printStyle)
  {
    this.value = value;
    this.justify = justify;
    this.hAlign = hAlign;
    this.vAlign = vAlign;
    this.printStyle = printStyle;
  }

  @NeverNull
  public String getValue() {
    return this.value;
  }

  @MaybeNull
  public MxlLeftCenterRight getJustify() {
    return this.justify;
  }

  @MaybeNull
  public MxlLeftCenterRight getHAlign() {
    return this.hAlign;
  }

  @MaybeNull
  public MxlVAlign getVAlign() {
    return this.vAlign;
  }

  @NeverNull
  public MxlPrintStyle getPrintStyle() {
    return this.printStyle;
  }

  @NeverNull
  public static MxlFormattedText read(Element e) {
    return new MxlFormattedText(XMLReader.textUntrimmed(e), MxlLeftCenterRight.read(e, "justify"), MxlLeftCenterRight.read(e, "halign"), MxlVAlign.read(e), MxlPrintStyle.read(e));
  }

  public void write(Element e)
  {
    e.setTextContent(this.value);
    if (this.justify != null)
      this.justify.write(e, "justify");
    if (this.hAlign != null)
      this.hAlign.write(e, "halign");
    if (this.vAlign != null)
      this.vAlign.write(e);
    this.printStyle.write(e);
  }
  
  public void paint(PaintTransfer pt){
	  String textContent = this.getValue();
		Paint paint = PaintUtil.getPaint(this,pt.getPaint());
		pt.setPaint(paint);
		MxlPosition mxlPos = this.getPrintStyle().getPosition();
		Point point = pt.getPointFromMxlPosition(mxlPos);
		pt.drawText(textContent, point.x, point.y);
  }
}