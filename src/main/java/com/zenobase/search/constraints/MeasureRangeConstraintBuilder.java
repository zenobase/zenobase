package com.zenobase.search.constraints;

import com.google.common.collect.Range;
import com.zenobase.common.MeasureRangeParser;
import com.zenobase.common.Measures;
import com.zenobase.json.DecimalMeasureField;
import com.zenobase.json.Field;
import javax.measure.DecimalMeasure;
import javax.measure.Measurable;
import javax.measure.quantity.Quantity;
import org.jspecify.annotations.Nullable;

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
		return Measures.toStandard((DecimalMeasure<Quantity>) value).getValue();
	}
}
