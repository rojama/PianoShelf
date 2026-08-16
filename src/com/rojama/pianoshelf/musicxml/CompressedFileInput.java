package com.rojama.pianoshelf.musicxml;

import com.rojama.pianoshelf.DebugLog;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.w3c.dom.Document;

public class CompressedFileInput {
	private File osTempFolder;
	private File tempFolder;
	private Object rootItem;

	public CompressedFileInput(InputStream inputStream, File osTempFolder) throws IOException {
		this.osTempFolder = osTempFolder;
		DebugLog.d("Compressed", "[MXL] 创建临时目录 + ZIP 解压中… osTempFolder=" + osTempFolder
				+ "  exists=" + (osTempFolder == null ? null : osTempFolder.exists())
				+ "  isDir=" + (osTempFolder == null ? null : osTempFolder.isDirectory()));

		this.tempFolder = new File(osTempFolder, UUID.randomUUID().toString());

		try {
			// 确保临时目录存在（含上级），否则 ZipTools.extractAll 写文件时 parent 不存在会 FNFE
			if (!this.tempFolder.exists() && !this.tempFolder.mkdirs()) {
				throw new IOException("Could not create temp folder: " + this.tempFolder);
			}

			// 自己用 ZipInputStream 解压，每条 entry 先 mkdirs，替代 util.jar 里 ZipTools.extractAll
			// （ZipTools.extractAll 遇到 images/88.svg 这种 entry 里 parent 非目录 ZIP entry 的情况
			//  直接 new FileOutputStream 会 FNFE，且错误里不带具体 entry 名，排查很痛苦）
			extractAllSafe(inputStream, this.tempFolder);
			DebugLog.i("Compressed", "[MXL] ZIP 解压完成到 " + this.tempFolder
					+ "，文件数=" + (this.tempFolder.listFiles() == null ? -1 : countFiles(this.tempFolder)));
			Document doc;
			try {
				doc = XMLReader.readFile(SafeXmlStream.wrap(new FileInputStream(new File(this.tempFolder,
						"META-INF/container.xml"))));
			} catch (Exception ex) {
				DebugLog.e("Compressed", "[MXL] container.xml 解析失败", ex);
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
			DebugLog.i("Compressed", "[MXL] rootfile/@full-path=" + rootfilePath);

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

	/**
	 * 安全解压 ZIP：对每个 entry 都先确保 parent 文件目录存在 (mkdirs)。
	 * util.jar 里的 ZipTools.extractAll 在某些 ZIP 里（目录名作为 entry 但不是 isDirectory 标记）
	 * 会直接 FNFE，此实现兼容 BeetAnGeSample.mxl / MozartTrio.mxl 等 MXL samples。
	 */
	private static void extractAllSafe(InputStream in, File outFolder) throws IOException {
		ZipInputStream zis = (in instanceof ZipInputStream)
				? (ZipInputStream) in : new ZipInputStream(new BufferedInputStream(in, 65536));
		byte[] buf = new byte[65536];
		ZipEntry ze;
		int entries = 0;
		while ((ze = zis.getNextEntry()) != null) {
			String name = ze.getName();
			// 安全过滤：不允许 ../ 穿越到 outFolder 之外
			if (name.contains("..")) {
				DebugLog.w("Compressed", "  [skip insecure zip entry] name=" + name);
				zis.closeEntry();
				continue;
			}
			File target = new File(outFolder, name);
			// 防 ZIP 绝对路径
			String outCanonical = outFolder.getCanonicalPath();
			String tCanonical = target.getCanonicalPath();
			if (!tCanonical.startsWith(outCanonical + File.separator) && !tCanonical.equals(outCanonical)) {
				DebugLog.w("Compressed", "  [skip outside entry] name=" + name);
				zis.closeEntry();
				continue;
			}
			if (ze.isDirectory()) {
				if (!target.exists() && !target.mkdirs()) {
					throw new IOException("mkdirs failed for zip dir entry: " + target);
				}
				zis.closeEntry();
				continue;
			}
			File parent = target.getParentFile();
			if (parent != null && !parent.exists() && !parent.mkdirs()) {
				throw new IOException("mkdirs failed for: " + parent + "  entry=" + name);
			}
			long t0 = System.currentTimeMillis();
			long written = 0;
			try (OutputStream fos = new FileOutputStream(target)) {
				int n;
				while ((n = zis.read(buf)) > 0) {
					fos.write(buf, 0, n);
					written += n;
				}
			}
			long t1 = System.currentTimeMillis();
			DebugLog.v("Compressed", "  extract entry " + name + "  size=" + written
					+ "  time=" + (t1 - t0) + "ms");
			entries++;
			zis.closeEntry();
		}
		try { zis.close(); } catch (IOException ignore) {}
		DebugLog.d("Compressed", "extractAllSafe 共解压 " + entries + " 个文件到 " + outFolder);
	}

	private static int countFiles(File f) {
		int c = 0;
		File[] children = f.listFiles();
		if (children == null) return c;
		for (File x : children) c += x.isDirectory() ? countFiles(x) : 1;
		return c;
	}
}