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
		Range<ReadableDuration> range = new DurationRangeParser().parse(value);
		RangeQueryBuilder query = QueryBuilders.rangeQuery(field);
		if (range.hasLowerBound()) {
			if (range.lowerBoundType() == BoundType.CLOSED) {
				query = query.gte(range.lowerEndpoint().getMillis());
			} else {
				checkBoundType(BoundType.OPEN, range.lowerBoundType());
				query = query.gt(range.lowerEndpoint().getMillis());
			}
		}
		if (range.hasUpperBound()) {
			if (range.upperBoundType() == BoundType.CLOSED) {
				query = query.lte(range.upperEndpoint().getMillis());
			} else {
				checkBoundType(BoundType.OPEN, range.upperBoundType());
				query = query.lt(range.upperEndpoint().getMillis());
			}
		}
		return query;
	}

	private static void checkBoundType(BoundType expected, BoundType actual) {
		Preconditions.checkState(expected == actual, "Expected <%s> but got <%s>", expected, actual);
	}
}
