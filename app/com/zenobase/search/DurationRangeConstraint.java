package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.joda.time.ReadableDuration;
import com.google.common.base.Preconditions;
import com.google.common.collect.BoundType;
import com.google.common.collect.Range;

import com.zenobase.common.DurationRangeParser;

public class DurationRangeConstraint implements Constraint {

	@Override
	public QueryBuilder build(String field, String value) {
		Range<ReadableDuration> range = parseRange(value);
		RangeQueryBuilder query = QueryBuilders.rangeQuery(getField(field));
		if (range.hasLowerBound()) {
			if (range.lowerBoundType() == BoundType.CLOSED) {
				query = query.gte(getValue(range.lowerEndpoint()));
			} else {
				checkBoundType(BoundType.OPEN, range.lowerBoundType());
				query = query.gt(getValue(range.lowerEndpoint()));
			}
		}
		if (range.hasUpperBound()) {
			if (range.upperBoundType() == BoundType.CLOSED) {
				query = query.lte(getValue(range.upperEndpoint()));
			} else {
				checkBoundType(BoundType.OPEN, range.upperBoundType());
				query = query.lt(getValue(range.upperEndpoint()));
			}
		}
		return query;
	}

	private Range<ReadableDuration> parseRange(String value) {
		return new DurationRangeParser().parse(value);
	}

	private String getField(String name) {
		return name;
	}

	private Long getValue(ReadableDuration value) {
		return value.getMillis();
	}

	private static void checkBoundType(BoundType expected, BoundType actual) {
		Preconditions.checkState(expected == actual, "Expected <%s> but got <%s>", expected, actual);
	}
}
