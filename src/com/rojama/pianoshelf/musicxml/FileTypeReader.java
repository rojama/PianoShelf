package com.rojama.pianoshelf.musicxml;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.xenoage.util.xml.NoResolver;
import com.xenoage.util.xml.XMLReader;

public class FileTypeReader {
	public static FileType getFileType(InputStream inputStream) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(inputStream, FileReader.BUFFER_SIZE);
		bis.mark(2);

		int[] bytes = { bis.read(), bis.read() };
		if ((bytes[0] == 80) && (bytes[1] == 75)) {
			return FileType.Compressed;
		}
		bis.reset();
		try {
			Document doc = XMLReader.readFile(bis);	        
			// root element
			Element root = doc.getDocumentElement();
			String name = root.getNodeName();
			if (name.equals("score-partwise"))
				return FileType.XMLScorePartwise;
			if (name.equals("score-timewise"))
				return FileType.XMLScoreTimewise;
			if (!name.equals("opus"))
				return FileType.XMLOpus;
						
		} catch (Exception ex) {
			return null;
		}

		return null;
	}
}