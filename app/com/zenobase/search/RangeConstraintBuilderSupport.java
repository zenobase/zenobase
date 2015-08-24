package com.zenobase.search;

import com.google.common.base.Preconditions;
import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;

public abstract class RangeConstraintBuilderSupport<C extends Comparable<C>> extends ConstraintBuilder {

	public RangeConstraintBuilderSupport(String path) {
		super(path);
	}

	@Override
	public QueryBuilder build(String value) {
		Range<C> range = parseRange(value);
		return range != null ? build(range) : null;
	}

	public QueryBuilder build(Range<C> range) {
		RangeQueryBuilder query = QueryBuilders.rangeQuery(getPath());
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

	protected abstract Object getValue(C value);

	protected static void checkBoundType(BoundType expected, BoundType actual) {
		Preconditions.checkState(expected == actual, "Expected <%s> but got <%s>", expected, actual);
	}
}
