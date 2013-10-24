package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

public class WildcardConstraintBuilder extends ConstraintBuilder {

	public WildcardConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public QueryBuilder build(String value) {
		return containsWildcard(value) ? QueryBuilders.wildcardQuery(getPath(), value) : null;
	}

	private static boolean containsWildcard(String value) {
		return value.indexOf('*') != -1;
	}
}
