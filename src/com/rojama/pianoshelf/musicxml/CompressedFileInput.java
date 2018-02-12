package com.rojama.pianoshelf.musicxml;

import com.xenoage.util.FileTools;
import com.xenoage.util.exceptions.InvalidFormatException;
import com.xenoage.util.xml.XMLReader;
import com.xenoage.util.zip.ZipTools;
import com.xenoage.zong.io.musicxml.link.LinkAttributes;
import com.xenoage.zong.io.musicxml.opus.Opus;
import com.xenoage.zong.io.musicxml.opus.OpusItem;
import com.xenoage.zong.musicxml.types.MxlScorePartwise;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.w3c.dom.Document;

public class CompressedFileInput {
	private File osTempFolder;
	private File tempFolder;
	private Object rootItem;

	public CompressedFileInput(InputStream inputStream, File osTempFolder) throws IOException {
		this.osTempFolder = osTempFolder;

		this.tempFolder = new File(osTempFolder, UUID.randomUUID().toString());
		
		try {
			if (!this.tempFolder.mkdir()) {
				throw new IOException("Could not create temp folder: " + this.tempFolder);
			}

			ZipTools.extractAll(inputStream, this.tempFolder);
			Document doc;
			try {
				doc = XMLReader.readFile(new FileInputStream(new File(this.tempFolder,
						"META-INF/container.xml")));
			} catch (Exception ex) {
				throw new IllegalStateException(
						"Compressed MusicXML file has no (well-formed) META-INF/container.xml", ex);
			}

			String rootfilePath;
			try {
				rootfilePath = XMLReader.element(XMLReader.element(XMLReader.root(doc), "rootfiles"),
						"rootfile").getAttribute("full-path");
			} catch (Exception ex) {
				throw new IllegalStateException("Invalid META-INF/container.xml", ex);
			}

			File rootfile = new File(this.tempFolder, rootfilePath);

			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(rootfile),
					FileReader.BUFFER_SIZE);
			try {
				FileType type = FileTypeReader.getFileType(bis);

				if (type == null)
					throw new IllegalStateException("Unknown root file type");
				switch (type) {
				case Compressed:
					throw new IllegalStateException("Root file may (currently) not be compressed");
				case XMLOpus:
					bis = new BufferedInputStream(new FileInputStream(rootfile), FileReader.BUFFER_SIZE);
					this.rootItem = new OpusFileInput().readOpusFile(bis);
					break;
				case XMLScorePartwise:
				case XMLScoreTimewise:
					this.rootItem = new LinkAttributes(rootfilePath);
				}
			} catch (IOException ex) {
				throw new IllegalStateException("Could not load root file", ex);
			}
		} catch (IOException e) {
			this.close();
			throw e;			
		}
	}

	public Object getRootItem() {
		return this.rootItem;
	}

	public boolean isOpus() {
		return this.rootItem instanceof Opus;
	}

	public List<String> getScoreFilenames() throws IOException {
		LinkedList<String> ret = new LinkedList<String>();
		if (isOpus()) {
			getScoreFilenames(new OpusFileInput().resolveOpusLinks((Opus) this.rootItem,
					this.tempFolder.getAbsolutePath()), ret);
		} else {
			ret.add(((LinkAttributes) this.rootItem).getHref());
		}
		return ret;
	}

	public MxlScorePartwise loadScore(String path) throws InvalidFormatException, IOException {
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(
				this.tempFolder, path)), FileReader.BUFFER_SIZE);

		FileType fileType = FileTypeReader.getFileType(bis);

		if (fileType == null)
			throw new InvalidFormatException("Score has invalid format: " + path);
		switch (fileType) {
		case Compressed:
			return loadCompressedScore(path);
		case XMLScorePartwise:
			bis = new BufferedInputStream(new FileInputStream(new File(this.tempFolder, path)),
					FileReader.BUFFER_SIZE);
			return new MusicXMLScoreFileInput().read(bis, path, null);
		case XMLScoreTimewise:
			throw new IllegalStateException("score-timewise is currently not implemented");
		case XMLOpus:
		}
		throw new InvalidFormatException("Score has invalid format: " + path);
	}

	private MxlScorePartwise loadCompressedScore(String path) throws IOException {
		CompressedFileInput zip = null;
		MxlScorePartwise ret = null;
		try {
			zip = new CompressedFileInput(new FileInputStream(new File(this.tempFolder, path)),
					this.osTempFolder);
			ret = zip.loadScore(((LinkAttributes) zip.getRootItem()).getHref());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (zip != null) zip.close();
		}
		return ret;
	}

	private void getScoreFilenames(Opus resolvedOpus, LinkedList<String> acc) {
		for (OpusItem item : resolvedOpus.getItems()) {
			if ((item instanceof com.xenoage.zong.io.musicxml.opus.Score))
				acc.add(((LinkAttributes) item).getHref());
			else if ((item instanceof Opus))
				getScoreFilenames((Opus) item, acc);
		}
	}

	public void close() {
		FileTools.deleteDirectory(this.tempFolder);
	}

	@Override
	public void finalize() {
		close();
	}
}