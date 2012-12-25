package com.zenobase.common;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DurationFieldType;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormatter;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class Intervals extends DateTimeFormatSupport {

	private enum IntervalType {

		YEAR(DurationFieldType.years()),
		MONTH(DurationFieldType.months()),
		DAY(DurationFieldType.days()),
		HOUR(DurationFieldType.hours()),
		MINUTE(DurationFieldType.minutes()),
		SECOND(DurationFieldType.seconds()),
		MILLISECOND(DurationFieldType.millis());

		private final DateTimeFormatter format;
		private final int length;
		private final Period period;

		private IntervalType(DurationFieldType type) {
			this.format = CustomDateTimeFormat.format(type);
			this.length = format.getParser().estimateParsedLength() - 1;
			this.period = new Period().withField(type, 1);
		}

		public Interval toInterval(String value) {
			return toInterval(format.parseDateTime(value));
		}

		private Interval toInterval(DateTime start) {
			return new Interval(start, start.plus(period));
		}

		private String toString(DateTime time) {
			return format.print(time);
		}

		public List<DateTime> toList(Interval interval) {
			List<DateTime> instants = Lists.newArrayList();
			for (DateTime start = interval.getStart(); interval.contains(start); start = start.plus(period)) {
				Preconditions.checkState(instants.size() < 1000, "Interval is too large: %s", interval);
				instants.add(start);
			}
			return instants;
		}
	}

	private Intervals() {
		throw new AssertionError();
	}

	public static Interval valueOf(String value) {
		value = value.replaceAll("Z", "+00:00");
		for (IntervalType format : IntervalType.values()) {
			if (value.length() == format.length) {
				return format.toInterval(value);
			}
		}
		throw new IllegalArgumentException("Unsupported date/time format: " + value);
	}

	public static String toString(DateTime time, String interval) {
		IntervalType format = IntervalType.valueOf(interval.toUpperCase());
		Preconditions.checkNotNull(format, "Unsupported interval: %s", interval);
		return format.toString(time);
	}

	public static List<DateTime> expand(DateTime start, DateTime end, String interval) {
		IntervalType format = IntervalType.valueOf(interval.toUpperCase());
		Preconditions.checkNotNull(format, "Unsupported interval: %s", interval);
		return format.toList(new Interval(start, end));
	}
}
