package com.zenobase.common;

import org.joda.time.ReadableInstant;

public class DateTimeRangeParser extends RangeParser<ReadableInstant> {

	@Override
	protected ReadableInstant getValue(String s) {
		return CustomDateTimeFormat.parse(s);
	}
}
