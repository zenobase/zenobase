package com.zenobase.common;

public class OffsetIntervalRangeParser extends RangeParser<ComparableInterval> {

	@Override
	protected ComparableInterval getValue(String s) {
		return OffsetDateTimeFormat.hasOffset(s) ? new ComparableInterval(OffsetIntervals.valueOf(s)) : null;
	}
}
