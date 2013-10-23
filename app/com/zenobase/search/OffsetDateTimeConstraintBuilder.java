package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.joda.time.Interval;

import com.zenobase.common.OffsetDateTimeFormat;
import com.zenobase.common.OffsetIntervals;

public class OffsetDateTimeConstraintBuilder implements ConstraintBuilder {

	@Override
	public QueryBuilder build(String field, String value) {
		return OffsetDateTimeFormat.hasOffset(value) ? build(field, OffsetIntervals.valueOf(value)) : null;
	}

	private static QueryBuilder build(String field, Interval interval) {
		if (interval == null) {
			return null;
		} else if (interval.toDurationMillis() > 1L) {
			return QueryBuilders.rangeQuery(field).gte(interval.getStart().toString()).lt(interval.getEnd().toString());
		} else {
			return QueryBuilders.termQuery(field, interval.getStart().toString());
		}
	}
}
