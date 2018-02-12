package com.xenoage.zong.io.musicxml.opus;

import com.xenoage.zong.io.musicxml.link.LinkAttributes;

public class Score implements OpusItem {
	private final LinkAttributes link;
	private final Boolean newPage;

	public Score(LinkAttributes link, Boolean newPage) {
		this.link = link;
		this.newPage = newPage;
	}

	public String getHref() {
		return this.link.getHref();
	}

	public Boolean isNewPage() {
		return this.newPage;
	}
}