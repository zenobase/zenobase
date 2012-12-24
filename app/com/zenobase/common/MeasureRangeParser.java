package com.zenobase.common;

import javax.measure.DecimalMeasure;
import javax.measure.Measurable;

public class MeasureRangeParser extends RangeParser<Measurable<?>> {

	@Override
	protected DecimalMeasure<?> getValue(String s) {
		return Measures.valueOf(s);
	}
}
