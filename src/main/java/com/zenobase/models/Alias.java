package com.zenobase.models;

import org.jspecify.annotations.Nullable;

public record Alias(String id, @Nullable String filter) {
	public Alias(String id) {
		this(id, null);
	}

	@Override
	public String toString() {
		return id;
	}
}
