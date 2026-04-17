package com.zenobase.search.constraints;

import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch._types.query_dsl.Query;

public abstract class ConstraintBuilder {

	private final String path;

	protected ConstraintBuilder(String path) {
		this.path = path;
	}

	protected String getPath() {
		return path;
	}

	public abstract @Nullable Query build(String value);
}
