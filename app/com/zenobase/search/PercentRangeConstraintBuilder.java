package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;

public class PercentRangeConstraintBuilder extends DecimalRangeConstraintBuilder {

	@Override
	public QueryBuilder build(String field, String value) {
		return super.build(field, value.replaceAll("%", ""));
	}
}
