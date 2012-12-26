package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.joda.time.Duration;

import com.zenobase.common.DurationFormat;

public class DurationConstraint implements Constraint {

	@Override
	public QueryBuilder build(String field, String value) {
		return build(field, DurationFormat.parse(value));
	}

	private static QueryBuilder build(String field, Duration value) {
		return QueryBuilders.termQuery(field, value.getMillis());
	}
}
