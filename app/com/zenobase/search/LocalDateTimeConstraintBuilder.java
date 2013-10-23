package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import com.zenobase.common.LocalInterval;
import com.zenobase.common.LocalIntervals;

public class LocalDateTimeConstraintBuilder implements ConstraintBuilder {

	private final String field;

	public LocalDateTimeConstraintBuilder(String field) {
		this.field = field;
	}

	@Override
	public QueryBuilder build(String field, String value) {
		LocalInterval interval = LocalIntervals.valueOf(value);
		return interval.toDurationMillis() > 1L
			? QueryBuilders.rangeQuery(this.field).gte(toString(interval.getStart())).lt(toString(interval.getEnd()))
			: QueryBuilders.termQuery(this.field, toString(interval.getStart()));
	}

	private static String toString(LocalDateTime value) {
		return value.toDateTime(DateTimeZone.UTC).toString();
	}
}
