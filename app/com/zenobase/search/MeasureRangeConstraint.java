package com.zenobase.search;

import javax.measure.DecimalMeasure;
import javax.measure.Measurable;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import com.google.common.base.Preconditions;
import com.google.common.collect.BoundType;
import com.google.common.collect.Range;

import com.zenobase.common.MeasureRangeParser;
import com.zenobase.common.Measures;
import com.zenobase.json.MeasurementField;

public class MeasureRangeConstraint implements Constraint {

	@Override
	public QueryBuilder build(String field, String value) {
		Range<Measurable<?>> range = new MeasureRangeParser().parse(value);
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

	private String getField(String name) {
		return name + "." + MeasurementField.VALUE_SI.getName();
	}

	private Number getValue(Measurable<?> value) {
		return Measures.toStandard(((DecimalMeasure<?>) value)).getValue();
	}

	private static void checkBoundType(BoundType expected, BoundType actual) {
		Preconditions.checkState(expected == actual, "Expected <%s> but got <%s>", expected, actual);
	}
}
