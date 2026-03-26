package com.zenobase.common;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;

public class DateTimeZones {

	private DateTimeZones() {}

	/**
	 * Converts a local time to the nearest time that exists in the specified time zone.
	 */
	public static DateTime toDateTime(LocalDateTime local, DateTimeZone timezone) {
		return new DateTime(
				DateTimeZone.UTC.getMillisKeepLocal(
						timezone, local.toDateTime(DateTimeZone.UTC).getMillis()),
				timezone);
	}
}
