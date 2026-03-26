package com.zenobase.search;

import com.google.common.base.Preconditions;
import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.RangeQuery;

public abstract class RangeConstraintBuilderSupport<C extends Comparable<C>> extends ConstraintBuilder {

	public RangeConstraintBuilderSupport(String path) {
		super(path);
	}

	@Override
	public @Nullable Query build(String value) {
		Range<C> range = parseRange(value);
		return range != null ? build(range) : null;
	}

	public Query build(Range<C> range) {
		RangeQuery.Builder builder = new RangeQuery.Builder().field(getPath());
		if (range.hasLowerBound()) {
			JsonData val = JsonData.of(getValue(range.lowerEndpoint()));
			if (range.lowerBoundType() == BoundType.CLOSED) {
				builder.gte(val);
			} else {
				checkBoundType(BoundType.OPEN, range.lowerBoundType());
				builder.gt(val);
			}
		}
		if (range.hasUpperBound()) {
			JsonData val = JsonData.of(getValue(range.upperEndpoint()));
			if (range.upperBoundType() == BoundType.CLOSED) {
				builder.lte(val);
			} else {
				checkBoundType(BoundType.OPEN, range.upperBoundType());
				builder.lt(val);
			}
		}
		return Query.of(q -> q.range(builder.build()));
	}

	protected abstract @Nullable Range<C> parseRange(String value);

	protected abstract Object getValue(C value);

	protected static void checkBoundType(BoundType expected, BoundType actual) {
		Preconditions.checkState(expected == actual, "Expected <%s> but got <%s>", expected, actual);
	}
}
