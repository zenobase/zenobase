package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;

public class PercentConstraintBuilder extends TermConstraintBuilder {

	public PercentConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public QueryBuilder build(String value) {
		return super.build(value.replaceAll("%", ""));
	}
}
