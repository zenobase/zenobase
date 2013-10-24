package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;

import com.zenobase.json.Field;

public class PercentRangeConstraintBuilder extends DecimalRangeConstraintBuilder {

	public PercentRangeConstraintBuilder(Field<?> field) {
		super(field);
	}

	@Override
	public QueryBuilder build(String value) {
		return super.build(value.replaceAll("%", ""));
	}
}
