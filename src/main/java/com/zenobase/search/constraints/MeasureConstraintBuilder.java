package com.zenobase.search.constraints;

import java.math.BigDecimal;

import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;

import com.zenobase.common.Measures;
import com.zenobase.json.DecimalMeasureField;
import com.zenobase.json.Field;

public class MeasureConstraintBuilder extends ConstraintBuilder {

	public MeasureConstraintBuilder(String path) {
		super(path);
	}

	@Override
	public Query build(String value) {
		return build(Measures.toStandard(Measures.valueOf(value)).getValue());
	}

	private Query build(BigDecimal value) {
		return Query.of(q -> q.term(t -> t.field(getPath()).value(FieldValue.of(value.doubleValue()))));
	}

	@Override
	protected String getPath() {
		return Field.concat(super.getPath(), DecimalMeasureField.VALUE_SI.getName());
	}
}
