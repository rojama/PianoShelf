package com.rojama.pianoshelf.musicxml;

import com.rojama.pianoshelf.DebugLog;
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
	private static final String TAG = "FileReader";

	public static List<MxlScorePartwise> loadScores(String path, Filter<String> scoreFileFilter)
			throws IOException {
		List ret = new LinkedList();
		DebugLog.i(TAG, "[1/5] loadScores 路径=" + path);
		try {
			String directory = FileTools.getDirectoryName(path);
			BufferedInputStream bis = new BufferedInputStream(IO.openInputStreamPreservePath(path),
					BUFFER_SIZE);
			FileType fileType = FileTypeReader.getFileType(bis);
			DebugLog.i(TAG, "[2/5] 文件类型判定: " + (fileType == null ? "null(未知)" : fileType.name()));
			bis = new BufferedInputStream(IO.openInputStreamPreservePath(path), BUFFER_SIZE);
			if (fileType == FileType.XMLScorePartwise) {
				DebugLog.d(TAG, "[3/5] XML(partwise) 解析…");
				MxlScorePartwise score = new MusicXMLScoreFileInput().read(bis, path, null);
				ret.add(score);
				DebugLog.i(TAG, "[4/5] XML(partwise) 解析 OK，第 1 页/总页数稍后在渲染阶段给出");
			} else if (fileType == FileType.XMLOpus) {
				DebugLog.d(TAG, "[3/5] Opus XML 解析…");
				OpusFileInput opusInput = new OpusFileInput();
				Opus opus = opusInput.readOpusFile(bis);
				opus = opusInput.resolveOpusLinks(opus, directory);
				List<String> filePaths = scoreFileFilter.filter(opus.getScoreFilenames());
				DebugLog.i(TAG, "[3/5] Opus 分解子文件数=" + filePaths.size());
				int idx = 0;
				for (String filePath : filePaths) {
					String relativePath = directory + "/" + filePath;
					List scores = loadScores(relativePath, scoreFileFilter);
					ret.addAll(scores);
					idx++;
					DebugLog.v(TAG, "[3/5] Opus 子文件 " + idx + "/" + filePaths.size() + " done (" + scores.size() + " scores)");
				}
			} else if (fileType == FileType.Compressed) {
				DebugLog.d(TAG, "[3/5] MXL 解压：ZIP → temp, 读取 container.xml → rootfile");
				CompressedFileInput zip = null;
				try {
					zip = new CompressedFileInput(bis, FileTools.getTempFolder());
					List<String> filePaths = scoreFileFilter.filter(zip.getScoreFilenames());
					DebugLog.i(TAG, "[3/5] container.xml 里 rootfile 总数=" + filePaths.size());
					int idx = 0;
					for (String filePath : filePaths) {
						DebugLog.d(TAG, "[3/5] 解析 rootfile(" + (idx + 1) + "/" + filePaths.size() + "): " + filePath);
						try {
							MxlScorePartwise score = zip.loadScore(filePath);
							if (score != null) {
								ret.add(score);
								DebugLog.i(TAG, "[3/5] rootfile OK: " + filePath);
							} else {
								DebugLog.w(TAG, "[3/5] rootfile 返回 null: " + filePath);
							}
						} catch (Throwable t) {
							DebugLog.e(TAG, "[3/5] rootfile 解析失败: " + filePath, t);
						}
						idx++;
					}
				} catch (Exception e) {
					DebugLog.e(TAG, "[3/5] MXL 解压/解析失败", e);
					throw e;
				} finally {
					if (zip != null) zip.close();
				}
			} else if (fileType == FileType.XMLScoreTimewise) {
				throw new IOException("score-timewise 格式目前未实现");
			} else {
				throw new IOException("未知文件格式 (fileType=null)，非 MusicXML/MXL/Opus");
			}
		} catch (IOException ioe) {
			DebugLog.e(TAG, "[X] loadScores IOException", ioe);
			throw ioe;
		} catch (RuntimeException re) {
			DebugLog.e(TAG, "[X] loadScores RuntimeException", re);
			throw re;
		}
		DebugLog.i(TAG, "[5/5] loadScores 完成，共解析到 " + ret.size() + " 个乐谱");
		return ret;
	}
}
