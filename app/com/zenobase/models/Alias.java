package com.zenobase.models;

public class Alias {

	private final String id;

	public Alias(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof Alias &&
			equals((Alias) that);
	}

	private boolean equals(Alias that) {
		return id.equals(that.getId());
	}

	@Override
	public String toString() {
		return id;
	}
}
