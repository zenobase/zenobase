package com.zenobase.models;

import com.google.common.base.Objects;

public class Alias {

	private final String id;
	private final String filter;

	public Alias(String id) {
		this(id, null);
	}

	public Alias(String id, String filter) {
		this.id = id;
		this.filter = filter;
	}

	public String getId() {
		return id;
	}

	public String getFilter() {
		return filter;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id, filter);
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof Alias &&
			equals((Alias) that);
	}

	private boolean equals(Alias that) {
		return id.equals(that.getId()) &&
			Objects.equal(filter, that.getFilter());
	}

	@Override
	public String toString() {
		return id;
	}
}
