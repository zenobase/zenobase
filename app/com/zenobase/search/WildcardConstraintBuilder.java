package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.zenobase.json.Field;

public class WildcardConstraintBuilder extends ConstraintBuilder {

	public WildcardConstraintBuilder(Field<?> field) {
		super(field);
	}

	@Override
	public QueryBuilder build(String value) {
		return containsWildcard(value) ? QueryBuilders.wildcardQuery(getField().getPath(), value) : null;
	}

	private static boolean containsWildcard(String value) {
		return value.indexOf('*') != -1;
	}
}
