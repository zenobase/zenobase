package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.joda.time.Interval;
import org.joda.time.format.ISODateTimeFormat;

import com.zenobase.common.Intervals;

public class DateTimeRangeConstraint implements Constraint {

	@Override
	public QueryBuilder build(String field, String value) {
		Interval interval = Intervals.valueOf(value);
		String from = interval.getStart().toString(ISODateTimeFormat.dateTime());
		String to = interval.getEnd().toString(ISODateTimeFormat.dateTime());
		return QueryBuilders.rangeQuery(field).gte(from).lt(to);
	}
}
