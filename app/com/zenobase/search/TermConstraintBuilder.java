package com.zenobase.search;

import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;

public class TermConstraintBuilder extends ConstraintBuilder {

	public TermConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public QueryBuilder build(String value) {
		return QueryBuilders.termQuery(getPath(), value);
	}
}
