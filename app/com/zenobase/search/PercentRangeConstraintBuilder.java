package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;

public class PercentRangeConstraintBuilder extends DecimalRangeConstraintBuilder {

	public PercentRangeConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public QueryBuilder build(String value) {
		return super.build(value.replaceAll("%", ""));
	}
}
