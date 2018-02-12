package com.xenoage.zong.io.musicxml.opus;

import com.rojama.pianoshelf.musicxml.OpusFileInput;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Opus implements OpusItem {
	private final String title;
	private final List<OpusItem> items;

	public Opus(String title, List<OpusItem> items) {
		this.title = title;
		this.items = items;
	}

	public String getTitle() {
		return this.title;
	}

	public List<OpusItem> getItems() {
		return this.items;
	}

	public List<String> getScoreFilenames() throws IOException {
		LinkedList ret = new LinkedList();
		getScoreFilenames(new OpusFileInput().resolveOpusLinks(this, ""), ret);
		return ret;
	}

	private void getScoreFilenames(Opus resolvedOpus, LinkedList<String> acc) {
		for (OpusItem item : resolvedOpus.getItems()) {
			if ((item instanceof Score))
				acc.add(((Score) item).getHref());
			else if ((item instanceof Opus))
				getScoreFilenames((Opus) item, acc);
		}
	}
}