package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.joda.time.Interval;

import com.zenobase.common.Intervals;

public class DateTimeConstraintBuilder implements ConstraintBuilder {

	@Override
	public QueryBuilder build(String field, String value) {
		Interval interval = Intervals.valueOf(value);
		return interval.toDurationMillis() > 1L
			? QueryBuilders.rangeQuery(field).gte(interval.getStart().toString()).lt(interval.getEnd().toString())
			: QueryBuilders.termQuery(field, interval.getStart().toString());
	}
}
