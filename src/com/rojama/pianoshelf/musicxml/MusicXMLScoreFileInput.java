package com.rojama.pianoshelf.musicxml;

import com.xenoage.util.FileTools;
import com.xenoage.util.error.BasicErrorProcessing;
import com.xenoage.util.exceptions.InvalidFormatException;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.zong.musicxml.MusicXMLDocument;
import com.xenoage.zong.musicxml.types.MxlScorePartwise;
import com.xenoage.zong.musicxml.util.InvalidMusicXML;

import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import org.w3c.dom.Document;

public class MusicXMLScoreFileInput {
	public String getFileFormatName() {
		return "MusicXML 2.0";
	}

	public FilenameFilter getFilenameFilter() {
		return FileTools.getXMLFilter();
	}

	public MxlScorePartwise read(InputStream inputStream, String filePath, BasicErrorProcessing err)
			throws InvalidFormatException, IOException {
		MxlScorePartwise score;
		try {
			Document xmlDoc = XMLReader.readFile(inputStream);
			MusicXMLDocument doc = MusicXMLDocument.read(xmlDoc);
			score = doc.getScore();
		} catch (InvalidMusicXML ex) {
			ex.printStackTrace();
			throw new InvalidFormatException(ex.getMessage());
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new IOException(ex.getMessage());
		}

		return score;
	}

}