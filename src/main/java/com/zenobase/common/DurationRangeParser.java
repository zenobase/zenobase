package com.zenobase.common;

import org.joda.time.ReadableDuration;

public class DurationRangeParser extends RangeParser<ReadableDuration> {

	@Override
	protected ReadableDuration getValue(String s) {
		return DurationFormat.parse(s);
	}
}
