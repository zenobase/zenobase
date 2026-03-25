package com.zenobase.models;

public record Alias(String id, String filter) {

	public Alias(String id) {
		this(id, null);
	}

	@Override
	public String toString() {
		return id;
	}
}
