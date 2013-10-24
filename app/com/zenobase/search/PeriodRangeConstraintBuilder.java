package com.zenobase.search;

import com.google.common.collect.Range;

import com.zenobase.common.PeriodRangeParser;
import com.zenobase.common.StandardPeriod;

public class PeriodRangeConstraintBuilder extends RangeConstraintBuilderSupport<StandardPeriod> {

	private final PeriodRangeParser parser = new PeriodRangeParser();

	public PeriodRangeConstraintBuilder(String path) {
		super(path);
	}

	@Override
	protected Range<StandardPeriod> parseRange(String value) {
		return parser.parse(value);
	}

	@Override
	protected String getValue(StandardPeriod value) {
		return "now" + value;
	}
}
