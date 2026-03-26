package com.zenobase.common;

import org.jspecify.annotations.Nullable;

public class PeriodRangeParser extends RangeParser<StandardPeriod> {

	@Override
	protected @Nullable StandardPeriod getValue(String s) {
		return StandardPeriod.hasPeriod(s) ? StandardPeriod.valueOf(s) : null;
	}
}
