package com.zenobase.search;

import java.math.BigDecimal;

import com.google.common.collect.Range;

import com.zenobase.common.DecimalRangeParser;

public class DecimalRangeConstraintBuilder extends RangeConstraintBuilderSupport<BigDecimal> {

	private final DecimalRangeParser parser = new DecimalRangeParser();

	public DecimalRangeConstraintBuilder(String path) {
		super(path);
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
