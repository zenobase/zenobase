package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;

public class PercentConstraintBuilder extends TermConstraintBuilder {

	@Override
	public QueryBuilder build(String field, String value) {
		return super.build(field, value.replaceAll("%", ""));
	}
}
