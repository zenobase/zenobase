package com.zenobase.common;

import org.jspecify.annotations.Nullable;

public class OffsetIntervalRangeParser extends RangeParser<ComparableInterval> {

	@Override
	protected @Nullable ComparableInterval getValue(String s) {
		var interval = OffsetIntervals.valueOf(s);
		return OffsetDateTimeFormat.hasOffset(s) && interval != null ? new ComparableInterval(interval) : null;
	}
}
