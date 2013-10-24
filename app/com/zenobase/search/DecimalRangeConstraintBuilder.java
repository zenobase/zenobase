package com.zenobase.search;

import java.math.BigDecimal;

import com.google.common.collect.Range;

import com.zenobase.common.DecimalRangeParser;
import com.zenobase.json.Field;

public class DecimalRangeConstraintBuilder extends RangeConstraintBuilderSupport<BigDecimal> {

	private final DecimalRangeParser parser = new DecimalRangeParser();

	public DecimalRangeConstraintBuilder(Field<?> field) {
		super(field);
	}

	@Override
	protected Range<BigDecimal> parseRange(String value) {
		return parser.parse(value);
	}

	@Override
	protected Number getValue(BigDecimal value) {
		return value;
	}
}
