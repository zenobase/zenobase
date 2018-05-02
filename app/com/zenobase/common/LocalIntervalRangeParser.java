package com.zenobase.common;

public class LocalIntervalRangeParser extends RangeParser<LocalInterval> {

	@Override
	protected LocalInterval getValue(String s) {
		return LocalIntervals.valueOf(s);
	}
}
