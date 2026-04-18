package com.zenobase.common;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

public class DateTimeZones {

	private DateTimeZones() {}

	/**
	 * Converts a local time to the nearest time that exists in the specified time zone.
	 */
	public static DateTime toDateTime(LocalDateTime local, @Nullable DateTimeZone timezone) {
		DateTimeZone zone = timezone != null ? timezone : DateTimeZone.UTC;
		return new DateTime(
			DateTimeZone.UTC.getMillisKeepLocal(zone, local.toDateTime(DateTimeZone.UTC).getMillis()),
			zone
		);
	}
}
