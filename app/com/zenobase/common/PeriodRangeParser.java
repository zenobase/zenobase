package com.zenobase.common;

public class PeriodRangeParser extends RangeParser<StandardPeriod> {

	@Override
	protected StandardPeriod getValue(String s) {
		return StandardPeriod.valueOf(s);
	}
}
