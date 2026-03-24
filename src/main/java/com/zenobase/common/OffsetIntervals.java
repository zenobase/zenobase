package com.zenobase.common;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DurationFieldType;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

public class OffsetIntervals extends DateTimeFormatSupport {

	private enum IntervalType {

		YEAR(DurationFieldType.years()) {
			@Override
			protected DateTimeFormatterBuilder configure(DateTimeFormatterBuilder builder) {
				return builder
					.append(yearElement())
					.appendLiteral('T')
					.append(offsetElement());
			}
		},

		MONTH(DurationFieldType.months()) {
			@Override
			protected DateTimeFormatterBuilder configure(DateTimeFormatterBuilder builder) {
				return builder
					.append(yearElement())
					.append(monthElement())
					.appendLiteral('T')
					.append(offsetElement());
			}
		},

		WEEK(DurationFieldType.weeks()) {
			@Override
			protected DateTimeFormatterBuilder configure(DateTimeFormatterBuilder builder) {
				return builder
					.append(yearElement())
					.append(weekofYearElement())
					.appendLiteral('T')
					.append(offsetElement());
			}
		},

		DAY(DurationFieldType.days()) {
			@Override
			protected DateTimeFormatterBuilder configure(DateTimeFormatterBuilder builder) {
				return builder
					.append(yearElement())
					.append(monthElement())
					.append(dayOfMonthElement())
					.appendLiteral('T')
					.append(offsetElement());
			}
		},

		HOUR(DurationFieldType.hours()) {
			@Override
			protected DateTimeFormatterBuilder configure(DateTimeFormatterBuilder builder) {
				return builder
					.append(yearElement())
					.append(monthElement())
					.append(dayOfMonthElement())
					.appendLiteral('T')
					.append(hourElement())
					.append(offsetElement());
			}
		},

		MINUTE(DurationFieldType.minutes()) {
			@Override
			protected DateTimeFormatterBuilder configure(DateTimeFormatterBuilder builder) {
				return builder
					.append(yearElement())
					.append(monthElement())
					.append(dayOfMonthElement())
					.appendLiteral('T')
					.append(hourElement())
					.append(minuteElement())
					.append(offsetElement());
			}
		},

		SECOND(DurationFieldType.seconds()) {
			@Override
			protected DateTimeFormatterBuilder configure(DateTimeFormatterBuilder builder) {
				return builder
					.append(yearElement())
					.append(monthElement())
					.append(dayOfMonthElement())
					.appendLiteral('T')
					.append(hourElement())
					.append(minuteElement())
					.append(secondElement())
					.append(offsetElement());
			}
		},

		MILLISECOND(DurationFieldType.millis()) {
			@Override
			protected DateTimeFormatterBuilder configure(DateTimeFormatterBuilder builder) {
				return builder
					.append(yearElement())
					.append(monthElement())
					.append(dayOfMonthElement())
					.appendLiteral('T')
					.append(hourElement())
					.append(minuteElement())
					.append(secondElement())
					.append(millisElement())
					.append(offsetElement());
			}
		};

		private final DateTimeFormatter format;
		private final int length;
		private final Period period;

		IntervalType(DurationFieldType type) {
			this.format = configure(new DateTimeFormatterBuilder()).toFormatter().withOffsetParsed();
			this.length = format.getParser().estimateParsedLength() - 1;
			this.period = new Period().withField(type, 1);
		}

		protected abstract DateTimeFormatterBuilder configure(DateTimeFormatterBuilder builder);

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
				if (instants.size() > 1440) {
					return Collections.emptyList();
				}
				instants.add(start);
			}
			return instants;
		}
	}

	private OffsetIntervals() {
		throw new AssertionError();
	}

	public static Interval valueOf(String value) {
		if (!value.isEmpty() && Character.isDigit(value.charAt(0))) {
			value = value.replaceAll("Z", "+00:00");
			for (IntervalType format : IntervalType.values()) {
				if (value.length() == format.length) {
					return format.toInterval(value);
				}
			}
		}
		return null;
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
