package com.xenoage.zong.io.musicxml.opus;

import com.xenoage.zong.io.musicxml.link.LinkAttributes;

public class OpusLink implements OpusItem {
	private final LinkAttributes link;

	public OpusLink(LinkAttributes link) {
		this.link = link;
	}

	public String getHref() {
		return this.link.getHref();
	}
}