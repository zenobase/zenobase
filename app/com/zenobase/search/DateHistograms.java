package com.zenobase.search;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.opensearch.client.opensearch._types.aggregations.CalendarInterval;

public class DateHistograms {

	private static final ImmutableMap<String, CalendarInterval> INTERVALS = ImmutableMap.<String, CalendarInterval>builder()
		.put("year", CalendarInterval.Year)
		.put("month", CalendarInterval.Month)
		.put("week", CalendarInterval.Week)
		.put("day", CalendarInterval.Day)
		.put("hour", CalendarInterval.Hour)
		.put("minute", CalendarInterval.Minute)
		.put("second", CalendarInterval.Second)
		.build();

	private DateHistograms() {

	}

	public static CalendarInterval parseInterval(String s) {
		CalendarInterval interval = INTERVALS.get(s);
		return Preconditions.checkNotNull(interval, "Invalid interval: %s", s);
	}

	public static long toEpochMillis(String bucketKey) {
		return Long.parseLong(bucketKey);
	}
}
