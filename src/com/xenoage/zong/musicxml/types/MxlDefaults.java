package com.xenoage.zong.musicxml.types;

import com.rojama.pianoshelf.PaintTransfer;
import com.xenoage.util.annotations.MaybeNull;
import com.xenoage.util.annotations.NeverNull;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.xml.XMLWriter;
import com.xenoage.zong.musicxml.types.attributes.MxlFont;
import com.xenoage.zong.musicxml.types.groups.MxlLayout;
import com.xenoage.zong.musicxml.util.IncompleteMusicXML;
import org.w3c.dom.Element;

@IncompleteMusicXML(missing="appearance,music-font,lyric-language", partly="lyric-font")
public final class MxlDefaults
{
  public static final String ELEM_NAME = "defaults";

  @MaybeNull
  private final MxlScaling scaling;

  @NeverNull
  private final MxlLayout layout;

  @MaybeNull
  private final MxlFont wordFont;

  @MaybeNull
  private final MxlLyricFont lyricFont;

  public MxlDefaults(MxlScaling scaling, MxlLayout layout, MxlFont wordFont, MxlLyricFont lyricFont)
  {
    this.scaling = scaling;
    this.layout = layout;
    this.wordFont = wordFont;
    this.lyricFont = lyricFont;
  }

  @MaybeNull
  public MxlScaling getScaling() {
    return this.scaling;
  }

  @NeverNull
  public MxlLayout getLayout() {
    return this.layout;
  }

  @MaybeNull
  public MxlFont getWordFont() {
    return this.wordFont;
  }

  @MaybeNull
  public MxlLyricFont getLyricFont() {
    return this.lyricFont;
  }

  @NeverNull
  public static MxlDefaults read(Element e) {
    MxlScaling scaling = null;
    MxlFont wordFont = null;
    MxlLyricFont lyricFont = null;
    for (Element c : XMLReader.elements(e))
    {
      String n = c.getNodeName();
      if (n.equals("scaling"))
        scaling = MxlScaling.read(c);
      else if (n.equals("word-font"))
        wordFont = MxlFont.read(c);
      else if ((n.equals("lyric-font")) && (lyricFont == null))
        lyricFont = MxlLyricFont.read(c);
    }
    return new MxlDefaults(scaling, MxlLayout.read(e), wordFont, lyricFont);
  }

  public void write(Element parent)
  {
    Element e = XMLWriter.addElement("defaults", parent);
    if (this.scaling != null)
      this.scaling.write(e);
    this.layout.write(e);
    XMLWriter.addElement("word-font", this.wordFont, e);
    XMLWriter.addElement("lyric-font", this.lyricFont, e);
  }
  
  public void paint(PaintTransfer pt) {
	  pt.ct.defaults = this;
	  this.getLayout().paint(pt);
	  //TODO
  }
}