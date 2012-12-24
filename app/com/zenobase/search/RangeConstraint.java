package com.zenobase.search;

import java.math.BigDecimal;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import com.google.common.collect.BoundType;
import com.google.common.collect.Range;

import com.zenobase.common.DecimalRangeParser;

public class RangeConstraint implements Constraint {

	@Override
	public QueryBuilder build(String field, String value) {
		Range<BigDecimal> range = new DecimalRangeParser().parse(value);
		RangeQueryBuilder query = QueryBuilders.rangeQuery(field);
		if (range.hasLowerBound()) {
			if (range.lowerBoundType() == BoundType.CLOSED) {
				query = query.gte(range.lowerEndpoint());
			} else if (range.lowerBoundType() == BoundType.OPEN) {
				query = query.gt(range.lowerEndpoint());
			}
		}
		if (range.hasUpperBound()) {
			if (range.upperBoundType() == BoundType.CLOSED) {
				query = query.lte(range.upperEndpoint());
			} else if (range.lowerBoundType() == BoundType.OPEN) {
				query = query.lt(range.upperEndpoint());
			}
		}
		return query;
	}
}
