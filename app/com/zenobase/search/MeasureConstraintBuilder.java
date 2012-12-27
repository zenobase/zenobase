package com.zenobase.search;

import java.math.BigDecimal;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.zenobase.common.Measures;
import com.zenobase.json.MeasurementField;

public class MeasureConstraintBuilder implements ConstraintBuilder {

	@Override
	public QueryBuilder build(String field, String value) {
		return build(field + "." + MeasurementField.VALUE_SI.getName(),
			Measures.toStandard(Measures.valueOf(value)).getValue());
	}

	private static QueryBuilder build(String field, BigDecimal value) {
		return QueryBuilders.termQuery(field, value);
	}
}
