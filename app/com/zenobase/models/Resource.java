package com.zenobase.models;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class Resource {

	private final String title;
	private final String url;

	public Resource(String title, String url) {
		Preconditions.checkNotNull(title);
		Preconditions.checkNotNull(url);
		this.title = title;
		this.url = url;
	}

	public String getTitle() {
		return title;
	}

	public String getUrl() {
		return url;
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof Resource &&
			equals((Resource) that);
	}

	private boolean equals(Resource that) {
		return title.equals(that.getTitle()) &&
			url.equals(that.getUrl());
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(title, url);
	}

	@Override
	public String toString() {
		return String.format("%s <%s>", title, url);
	}
}
