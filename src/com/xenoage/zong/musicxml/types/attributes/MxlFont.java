package com.xenoage.zong.musicxml.types.attributes;

import com.xenoage.pdlib.PVector;
import com.xenoage.util.StringTools;
import com.xenoage.util.annotations.MaybeEmpty;
import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.zong.musicxml.types.enums.MxlFontStyle;
import com.xenoage.zong.musicxml.types.enums.MxlFontWeight;
import org.w3c.dom.Element;

public final class MxlFont
{

  @MaybeEmpty
  private final PVector<String> fontFamily;

  @MaybeNull
  private final MxlFontStyle fontStyle;

  @MaybeNull
  private final MxlFontSize fontSize;

  @MaybeNull
  private final MxlFontWeight fontWeight;
  public static final MxlFont empty = new MxlFont(new PVector(), null, null, null);

  public MxlFont(PVector<String> fontFamily, MxlFontStyle fontStyle, MxlFontSize fontSize, MxlFontWeight fontWeight)
  {
    this.fontFamily = fontFamily;
    this.fontStyle = fontStyle;
    this.fontSize = fontSize;
    this.fontWeight = fontWeight;
  }

  @MaybeEmpty
  public PVector<String> getFontFamily()
  {
    return this.fontFamily;
  }

  @MaybeNull
  public MxlFontStyle getFontStyle() {
    return this.fontStyle;
  }

  @MaybeNull
  public MxlFontSize getFontSize() {
    return this.fontSize;
  }

  @MaybeNull
  public MxlFontWeight getFontWeight() {
    return this.fontWeight;
  }

  @NeverNull
  public static MxlFont read(Element e) {
    PVector fontFamily = PVector.pvec();
    String fontFamilies = XMLReader.attributeNotNull(e, "font-family");
    for (String s : fontFamilies.split(","))
    {
      fontFamily = fontFamily.plus(s.trim());
    }
    MxlFontStyle fontStyle = MxlFontStyle.read(e);
    MxlFontSize fontSize = MxlFontSize.read(e);
    MxlFontWeight fontWeight = MxlFontWeight.read(e);
    if ((fontFamily.size() > 0) || (fontStyle != null) || (fontSize != null) || (fontWeight != null))
    {
      return new MxlFont(fontFamily, fontStyle, fontSize, fontWeight);
    }

    return empty;
  }

  public void write(Element e)
  {
    if (this.fontFamily.size() > 0)
      e.setAttribute("font-family", StringTools.concatenate(this.fontFamily, ","));
    if (this.fontStyle != null)
      this.fontStyle.write(e);
    if (this.fontSize != null)
      this.fontSize.write(e);
    if (this.fontWeight != null)
      this.fontWeight.write(e);
  }
}