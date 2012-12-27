package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

public class TermConstraintBuilder implements ConstraintBuilder {

	@Override
	public QueryBuilder build(String field, String value) {
		return QueryBuilders.termQuery(field, value);
	}
}
