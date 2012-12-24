package com.zenobase.search;

import javax.measure.DecimalMeasure;
import javax.measure.Measurable;

import com.google.common.collect.Range;

import com.zenobase.common.MeasureRangeParser;
import com.zenobase.common.Measures;
import com.zenobase.json.MeasurementField;

public class MeasureRangeConstraint extends RangeConstraintSupport<Measurable<?>> {

	private final MeasureRangeParser parser = new MeasureRangeParser();

	@Override
	protected Range<Measurable<?>> parseRange(String value) {
		return parser.parse(value);
	}

	@Override
	protected String getField(String name) {
		return name + "." + MeasurementField.VALUE_SI.getName();
	}

	@Override
	protected Number getValue(Measurable<?> value) {
		return Measures.toStandard(((DecimalMeasure<?>) value)).getValue();
	}
}
