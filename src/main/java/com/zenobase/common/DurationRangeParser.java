package com.zenobase.common;

import org.joda.time.Duration;
import org.joda.time.ReadableDuration;

public class DurationRangeParser extends RangeParser<ReadableDuration> {

	@Override
	protected ReadableDuration getValue(String s) {
		return new Duration(DurationFormat.parse(s));
	}
}
