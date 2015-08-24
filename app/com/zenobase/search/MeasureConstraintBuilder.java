package com.zenobase.search;

import java.math.BigDecimal;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.zenobase.common.Measures;
import com.zenobase.json.DecimalMeasureField;
import com.zenobase.json.Field;

public class MeasureConstraintBuilder extends ConstraintBuilder {

	public MeasureConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public QueryBuilder build(String value) {
		return build(Measures.toStandard(Measures.valueOf(value)).getValue());
	}

	private QueryBuilder build(BigDecimal value) {
		return QueryBuilders.termQuery(getPath(), value);
	}

	@Override
	protected String getPath() {
		return Field.concat(super.getPath(), DecimalMeasureField.VALUE_SI.getName());
	}
}
