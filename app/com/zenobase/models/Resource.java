package com.zenobase.models;

public class Resource {

	private final String title;
	private final String url;

	public Resource(String title, String url) {
		this.title = title;
		this.url = url;
	}

	public String getTitle() {
		return title;
	}

	public String getUrl() {
		return url;
	}
}
