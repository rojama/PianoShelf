package com.xenoage.zong.musicxml;

import com.xenoage.util.xml.XMLReader;
import com.xenoage.zong.musicxml.types.MxlScorePartwise;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public final class MusicXMLDocument
{
  private MxlScorePartwise score;

  private MusicXMLDocument(MxlScorePartwise score)
  {
    this.score = score;
  }

  public static MusicXMLDocument read(Document doc)
    throws InvalidMusicXML
  {
    Element e = XMLReader.root(doc);
    String n = e.getNodeName();
    if (n.equals("score-partwise"))
      return new MusicXMLDocument(MxlScorePartwise.read(e));
    if (n.equals("score-timewise"))
      throw new IllegalArgumentException("Timewise scores are not supported.");
    throw new IllegalArgumentException("Unknown root element: " + n);
  }

  public MxlScorePartwise getScore()
  {
    return this.score;
  }
}