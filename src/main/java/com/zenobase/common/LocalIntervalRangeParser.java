package com.zenobase.common;

import org.jspecify.annotations.Nullable;

public class LocalIntervalRangeParser extends RangeParser<LocalInterval> {

	@Override
	protected @Nullable LocalInterval getValue(String s) {
		return LocalIntervals.valueOf(s);
	}
}
