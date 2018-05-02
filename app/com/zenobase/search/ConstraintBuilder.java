package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;

public abstract class ConstraintBuilder {

	private final String path;

	protected ConstraintBuilder(String path) {
		this.path = path;
	}

	protected String getPath() {
		return path;
	}

	public abstract QueryBuilder build(String value);
}
