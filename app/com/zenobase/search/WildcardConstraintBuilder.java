package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

public class WildcardConstraintBuilder implements ConstraintBuilder {

	@Override
	public QueryBuilder build(String field, String value) {
		return containsWildcard(value)
			? QueryBuilders.wildcardQuery(field, value)
			: null;
	}

	private static boolean containsWildcard(String value) {
		return value.indexOf('*') != -1;
	}
}
