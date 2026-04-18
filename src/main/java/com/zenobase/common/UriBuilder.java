package com.zenobase.common;

import java.net.URISyntaxException;
import org.apache.hc.core5.net.URIBuilder;

public class UriBuilder {

	private final URIBuilder builder;

	public UriBuilder(String uri) {
		try {
			builder = new URIBuilder(uri);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	public UriBuilder addParameter(String param, String value) {
		builder.addParameter(param, value);
		return this;
	}

	public String build() {
		try {
			return builder.build().toString();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}
