package com.zenobase.search;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram;

public class DateHistograms {

	private static final ImmutableMap<String, DateHistogram.Interval> INTERVALS = ImmutableMap.<String, DateHistogram.Interval>builder()
		.put("year", DateHistogram.Interval.YEAR)
		.put("month", DateHistogram.Interval.MONTH)
		.put("week", DateHistogram.Interval.WEEK)
		.put("day", DateHistogram.Interval.DAY)
		.put("hour", DateHistogram.Interval.HOUR)
		.put("minute", DateHistogram.Interval.MINUTE)
		.put("second", DateHistogram.Interval.SECOND)
		.build();

	private DateHistograms() {

	}

	public static DateHistogram.Interval parseInterval(String s) {
		DateHistogram.Interval interval = INTERVALS.get(s);
		return Preconditions.checkNotNull(interval, "Invalid interval: %s", s);
	}
}
