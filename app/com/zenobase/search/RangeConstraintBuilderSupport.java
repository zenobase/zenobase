package com.zenobase.search;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import com.google.common.base.Preconditions;
import com.google.common.collect.BoundType;
import com.google.common.collect.Range;

public abstract class RangeConstraintBuilderSupport<C extends Comparable<C>> implements ConstraintBuilder {

	@Override
	public QueryBuilder build(String field, String value) {
		Range<C> range = parseRange(value);
		return range != null ? build(field, range) : null;
	}

	private QueryBuilder build(String field, Range<C> range) {
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

	protected abstract Range<C> parseRange(String value);

	protected String getField(String name) {
		return name;
	}

	protected abstract Number getValue(C value);

	private static void checkBoundType(BoundType expected, BoundType actual) {
		Preconditions.checkState(expected == actual, "Expected <%s> but got <%s>", expected, actual);
	}
}
