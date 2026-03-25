package com.zenobase.models;

import java.util.Objects;

public record Resource(String title, String url) {

	public Resource {
		Objects.requireNonNull(title);
		Objects.requireNonNull(url);
	}

	@Override
	public String toString() {
		return String.format("%s <%s>", title, url);
	}
}
