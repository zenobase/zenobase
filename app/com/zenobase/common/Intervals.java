package com.zenobase.common;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Interval;
import org.joda.time.Minutes;
import org.joda.time.Months;
import org.joda.time.ReadablePeriod;
import org.joda.time.Seconds;
import org.joda.time.Years;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class Intervals {

	private enum Format {

		YEAR(DateTimeFormat.forPattern("yyyy'T'Z").withOffsetParsed(), 10, Years.years(1)),
		MONTH(DateTimeFormat.forPattern("yyyy-MM'T'Z").withOffsetParsed(), 13, Months.months(1)),
		DAY(DateTimeFormat.forPattern("yyyy-MM-dd'T'Z").withOffsetParsed(), 16, Days.days(1)),
		HOUR(DateTimeFormat.forPattern("yyyy-MM-dd'T'HHZ").withOffsetParsed(), 18, Hours.hours(1)),
		MINUTE(DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mmZ").withOffsetParsed(), 21, Minutes.minutes(1)),
		SECOND(DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ").withOffsetParsed(), 24, Seconds.seconds(1));

		private final DateTimeFormatter format;
		private final int length;
		private final ReadablePeriod unit;

		private Format(DateTimeFormatter format, int length, ReadablePeriod unit) {
			this.format = format;
			this.length = length;
			this.unit = unit;
		}

		public DateTime parse(String value) {
			return format.parseDateTime(value);
		}

		public Interval toInterval(DateTime start) {
			return new Interval(start, start.plus(unit));
		}

		public Interval toInterval(String value) {
			return toInterval(parse(value));
		}

		private String toString(DateTime time) {
			return format.print(time);
		}

		public List<DateTime> expand(Interval interval) {
			List<DateTime> times = Lists.newArrayList();
			for (DateTime start = interval.getStart(); interval.contains(start); start = start.plus(unit)) {
				times.add(start);
			}
			return times;
		}
	}

	private Intervals() {
		throw new AssertionError();
	}

	public static Interval valueOf(String value) {
		for (Format format : Format.values()) {
			if (value.length() == format.length) {
				return format.toInterval(value);
			}
		}
		throw new IllegalArgumentException("Unsupported date/time format: " + value);
	}

	public static String toString(DateTime time, String interval) {
		Format format = Format.valueOf(interval.toUpperCase());
		Preconditions.checkNotNull(format, "Unsupported interval: %s", interval);
		return format.toString(time);
	}

	public static List<DateTime> expand(DateTime start, DateTime end, String interval) {
		Format format = Format.valueOf(interval.toUpperCase());
		Preconditions.checkNotNull(format, "Unsupported interval: %s", interval);
		return format.expand(new Interval(start, end));
	}
}
