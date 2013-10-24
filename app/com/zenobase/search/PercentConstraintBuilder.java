package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;

import com.zenobase.json.Field;

public class PercentConstraintBuilder extends TermConstraintBuilder {

	public PercentConstraintBuilder(Field<?> field) {
		super(field);
	}

	@Override
	public QueryBuilder build(String value) {
		return super.build(value.replaceAll("%", ""));
	}
}
