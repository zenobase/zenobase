package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

import com.zenobase.common.LocalInterval;
import com.zenobase.common.LocalIntervals;

public class LocalDateTimeConstraintBuilder extends ConstraintBuilder {

	public LocalDateTimeConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public QueryBuilder build(String value) {
		return build(LocalIntervals.valueOf(value));
	}

	private QueryBuilder build(LocalInterval interval) {
		if (interval == null) {
			return null;
		} else if (interval.toDurationMillis() > 1L) {
			return QueryBuilders.rangeQuery(getPath()).gte(toString(interval.getStart())).lt(toString(interval.getEnd()));
		} else {
			return QueryBuilders.termQuery(getPath(), toString(interval.getStart()));
		}
	}

	private static String toString(LocalDateTime value) {
		return value.toDateTime(DateTimeZone.UTC).toString();
	}
}
