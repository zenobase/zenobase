package com.zenobase.search.constraints;

import javax.measure.DecimalMeasure;
import javax.measure.Measurable;
import javax.measure.quantity.Quantity;

import com.google.common.collect.Range;
import org.jspecify.annotations.Nullable;

import com.zenobase.common.MeasureRangeParser;
import com.zenobase.common.Measures;
import com.zenobase.json.DecimalMeasureField;
import com.zenobase.json.Field;

public class MeasureRangeConstraintBuilder extends RangeConstraintBuilderSupport<Measurable<Quantity>> {

	private final MeasureRangeParser parser = new MeasureRangeParser();

	public MeasureRangeConstraintBuilder(String path) {
		super(path);
	}

	@Override
	protected @Nullable Range<Measurable<Quantity>> parseRange(String value) {
		return parser.parse(value);
	}

	@Override
	protected String getPath() {
		return Field.concat(super.getPath(), DecimalMeasureField.VALUE_SI.getName());
	}

	@Override
	protected Number getValue(Measurable<Quantity> value) {
		return Measures.toStandard(((DecimalMeasure<Quantity>) value)).getValue();
	}
}
