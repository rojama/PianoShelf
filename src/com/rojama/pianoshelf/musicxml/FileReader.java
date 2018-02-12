package com.rojama.pianoshelf.musicxml;

import com.xenoage.util.FileTools;
import com.xenoage.util.StreamTools;
import com.xenoage.util.filter.Filter;
import com.xenoage.util.io.IO;
import com.xenoage.zong.io.musicxml.opus.Opus;
import com.xenoage.zong.musicxml.types.MxlScorePartwise;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class FileReader {
	static final int BUFFER_SIZE = 100;

	public static List<MxlScorePartwise> loadScores(String path, Filter<String> scoreFileFilter)
			throws IOException {
		List ret = new LinkedList();

		String directory = FileTools.getDirectoryName(path);
		BufferedInputStream bis = new BufferedInputStream(IO.openInputStreamPreservePath(path),
				BUFFER_SIZE);
		FileType fileType = FileTypeReader.getFileType(bis);
		bis = new BufferedInputStream(IO.openInputStreamPreservePath(path), BUFFER_SIZE);
		if (fileType == FileType.XMLScorePartwise) {
			MxlScorePartwise score = new MusicXMLScoreFileInput().read(bis, path, null);
			ret.add(score);
		} else if (fileType == FileType.XMLOpus) {
			OpusFileInput opusInput = new OpusFileInput();
			Opus opus = opusInput.readOpusFile(bis);
			opus = opusInput.resolveOpusLinks(opus, directory);
			List<String> filePaths = scoreFileFilter.filter(opus.getScoreFilenames());
			for (String filePath : filePaths) {
				String relativePath = directory + "/" + filePath;
				List scores = loadScores(relativePath, scoreFileFilter);
				ret.addAll(scores);
			}
		} else if (fileType == FileType.Compressed) {
			CompressedFileInput zip = null;
			try {
				zip = new CompressedFileInput(bis, FileTools.getTempFolder());
				List<String> filePaths = scoreFileFilter.filter(zip.getScoreFilenames());
				for (String filePath : filePaths) {
					MxlScorePartwise score = zip.loadScore(filePath);
					ret.add(score);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (zip != null) zip.close();
			}
		}
		return ret;
	}
}