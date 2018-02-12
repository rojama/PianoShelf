package com.rojama.pianoshelf.musicxml;

import com.xenoage.util.Parser;
import com.xenoage.util.exceptions.InvalidFormatException;
import com.xenoage.util.io.IO;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.zong.io.musicxml.link.LinkAttributes;
import com.xenoage.zong.io.musicxml.opus.Opus;
import com.xenoage.zong.io.musicxml.opus.OpusItem;
import com.xenoage.zong.io.musicxml.opus.OpusLink;
import com.xenoage.zong.io.musicxml.opus.Score;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class OpusFileInput {
	public Opus readOpusFile(InputStream inputStream) throws InvalidFormatException, IOException {
		Document doc;
		try {
			doc = XMLReader.readFile(inputStream);
		} catch (Exception ex) {
			throw new IOException("Opus file does not exist or has invalid format");
		}

		Element root = XMLReader.root(doc);
		if (!root.getNodeName().equals("opus"))
			throw new InvalidFormatException("No opus document");
		return readOpus(root);
	}

	private Opus readOpus(Element eOpus) {
		String title = XMLReader.elementText(eOpus, "title");
		LinkedList items = new LinkedList();
		for (Element e : XMLReader.elements(eOpus)) {
			String name = e.getNodeName();
			if (name.equals("opus"))
				items.add(readOpus(e));
			else if (name.equals("opus-link"))
				items.add(readOpusLink(e));
			else if (name.equals("score"))
				items.add(readScore(e));
		}
		return new Opus(title, items);
	}

	private OpusLink readOpusLink(Element eOpusLink) {
		String href = eOpusLink.getAttribute("xlink:href");
		return new OpusLink(new LinkAttributes(href));
	}

	private Score readScore(Element eScore) {
		String href = eScore.getAttribute("xlink:href");
		Boolean newPage = Parser.parseBooleanNullYesNo(eScore.getAttribute("new-page"));
		return new Score(new LinkAttributes(href), newPage);
	}

	public Opus resolveOpusLinks(Opus opus, String baseDirectory) throws InvalidFormatException,
			IOException {
		LinkedList resolvedItems = new LinkedList();
		for (OpusItem item : opus.getItems()) {
			OpusItem resolvedItem = item;
			if ((item instanceof OpusLink)) {
				Opus newOpus = readOpusFile(IO.openInputStreamPreservePath(baseDirectory + "/"
						+ ((OpusLink) item).getHref()));

				resolvedItem = resolveOpusLinks(newOpus, baseDirectory);
			}
			resolvedItems.add(resolvedItem);
		}
		return new Opus(opus.getTitle(), resolvedItems);
	}
}