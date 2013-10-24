package com.zenobase.search;

import java.math.BigDecimal;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.zenobase.common.Measures;
import com.zenobase.json.Field;
import com.zenobase.json.MeasurementField;

public class MeasureConstraintBuilder extends ConstraintBuilder {

	public MeasureConstraintBuilder(Field<?> field) {
		super(field);
	}

	@Override
	public QueryBuilder build(String value) {
		return build(Measures.toStandard(Measures.valueOf(value)).getValue());
	}

	private QueryBuilder build(BigDecimal value) {
		return QueryBuilders.termQuery(getPath(), value);
	}

	private String getPath() {
		return Field.concat(getField().getPath(), MeasurementField.VALUE_SI.getName());
	}
}
