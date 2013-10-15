package com.zenobase.common;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.ReadableInstant;

public class DateTimeRangeParser extends RangeParser<ReadableInstant> {

	@Override
	protected ReadableInstant getValue(String s) {
		DateTime value = null;
		if (s.charAt(0) == '+') {
			value = now().plus(PeriodFormat.parse(s.substring(1)).toPeriod());
		} else if (s.charAt(0) == '-') {
			value = now().minus(PeriodFormat.parse(s.substring(1)).toPeriod());
		} else if (Characters.isDigits(s)) {
			value = new DateTime(Long.parseLong(s), DateTimeZone.UTC);
		} else {
			value = DateTimeFormat.parse(s);
		}
		return value;
	}

	protected DateTime now() {
		return DateTime.now(DateTimeZone.UTC);
	}
}
