package com.zenobase.search;

import java.time.ZonedDateTime;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.opensearch.search.aggregations.bucket.histogram.DateHistogramInterval;

public class DateHistograms {

	private static final ImmutableMap<String, DateHistogramInterval> INTERVALS = ImmutableMap.<String, DateHistogramInterval>builder()
		.put("year", DateHistogramInterval.YEAR)
		.put("month", DateHistogramInterval.MONTH)
		.put("week", DateHistogramInterval.WEEK)
		.put("day", DateHistogramInterval.DAY)
		.put("hour", DateHistogramInterval.HOUR)
		.put("minute", DateHistogramInterval.MINUTE)
		.put("second", DateHistogramInterval.SECOND)
		.build();

	private DateHistograms() {

	}

	public static DateHistogramInterval parseInterval(String s) {
		DateHistogramInterval interval = INTERVALS.get(s);
		return Preconditions.checkNotNull(interval, "Invalid interval: %s", s);
	}

	public static long toEpochMillis(Object bucketKey) {
		if (bucketKey instanceof ZonedDateTime) {
			return ((ZonedDateTime) bucketKey).toInstant().toEpochMilli();
		}
		return ((Number) bucketKey).longValue();
	}
}
