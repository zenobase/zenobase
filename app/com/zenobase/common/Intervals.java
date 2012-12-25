package com.zenobase.common;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.ReadablePeriod;
import org.joda.time.format.DateTimeFormatter;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class Intervals {

	private enum Format {

		YEAR(CustomDateTimeFormat.inclYear(), 11, Period.years(1)),
		MONTH(CustomDateTimeFormat.inclMonth(), 14, Period.months(1)),
		DAY(CustomDateTimeFormat.inclDayOfMonth(), 17, Period.days(1)),
		HOUR(CustomDateTimeFormat.inclHour(), 19, Period.hours(1)),
		MINUTE(CustomDateTimeFormat.inclMinute(), 22, Period.minutes(1)),
		SECOND(CustomDateTimeFormat.inclSecond(), 25, Period.seconds(1)),
		MILLISECOND(CustomDateTimeFormat.inclMillis(), 29, Period.millis(1));

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
				Preconditions.checkState(times.size() < 1000, "Interval is too large: %s", interval);
				times.add(start);
			}
			return times;
		}
	}

	private Intervals() {
		throw new AssertionError();
	}

	public static Interval valueOf(String value) {
		value = value.replaceAll("Z", "+00:00");
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
