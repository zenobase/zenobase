package com.zenobase.search;

import javax.measure.DecimalMeasure;
import javax.measure.Measurable;
import javax.measure.quantity.Quantity;

import com.google.common.collect.Range;

import com.zenobase.common.MeasureRangeParser;
import com.zenobase.common.Measures;
import com.zenobase.json.Field;
import com.zenobase.json.MeasurementField;

public class MeasureRangeConstraintBuilder extends RangeConstraintBuilderSupport<Measurable<Quantity>> {

	private final MeasureRangeParser parser = new MeasureRangeParser();

	public MeasureRangeConstraintBuilder(Field<?> field) {
		super(field);
	}

	@Override
	protected Range<Measurable<Quantity>> parseRange(String value) {
		return parser.parse(value);
	}

	@Override
	protected String getPath() {
		return Field.concat(super.getPath(), MeasurementField.VALUE_SI.getName());
	}

	@Override
	protected Number getValue(Measurable<Quantity> value) {
		return Measures.toStandard(((DecimalMeasure<Quantity>) value)).getValue();
	}
}
