package com.zenobase.common;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.ReadableInstant;

public class DateTimeRangeParser extends RangeParser<ReadableInstant> {

	@Override
	protected ReadableInstant getValue(String s) {
		return Characters.isDigits(s)
			? new DateTime(Long.parseLong(s), DateTimeZone.UTC)
			: DateTimeFormat.parse(s);
	}
}
