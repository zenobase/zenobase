package com.zenobase.search;

import org.opensearch.client.opensearch._types.query_dsl.Query;

public class PercentConstraintBuilder extends TermConstraintBuilder {

	public PercentConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public Query build(String value) {
		return super.build(value.replaceAll("%", ""));
	}
}
