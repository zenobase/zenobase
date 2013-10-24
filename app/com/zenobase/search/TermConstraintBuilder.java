package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.zenobase.json.Field;

public class TermConstraintBuilder extends ConstraintBuilder {

	public TermConstraintBuilder(Field<?> field) {
		super(field);
	}

	@Override
	public QueryBuilder build(String value) {
		return QueryBuilders.termQuery(getField().getPath(), value);
	}
}
