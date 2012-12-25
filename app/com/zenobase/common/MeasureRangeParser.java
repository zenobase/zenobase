package com.zenobase.common;

import javax.measure.DecimalMeasure;
import javax.measure.Measurable;
import javax.measure.quantity.Quantity;

public class MeasureRangeParser extends RangeParser<Measurable<Quantity>> {

	@Override
	protected DecimalMeasure<Quantity> getValue(String s) {
		return Measures.valueOf(s);
	}
}
