package com.zenobase.common;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.joda.time.DurationFieldType;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

public class LocalIntervals extends DateTimeFormatSupport {

	private enum IntervalType {

		YEAR(DurationFieldType.years()) {
			@Override
			protected DateTimeFormatterBuilder configure(DateTimeFormatterBuilder builder) {
				return builder
					.append(yearElement());
			}
		},

		MONTH(DurationFieldType.months()) {
			@Override
			protected DateTimeFormatterBuilder configure(DateTimeFormatterBuilder builder) {
				return builder
					.append(yearElement())
					.append(monthElement());
			}
		},

		WEEK(DurationFieldType.weeks()) {
			@Override
			protected DateTimeFormatterBuilder configure(DateTimeFormatterBuilder builder) {
				return builder
					.append(weekyearElement())
					.append(weekofYearElement());
			}
		},

		DAY(DurationFieldType.days()) {
			@Override
			protected DateTimeFormatterBuilder configure(DateTimeFormatterBuilder builder) {
				return builder
					.append(yearElement())
					.append(monthElement())
					.append(dayOfMonthElement());
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
					.append(hourElement());
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
					.append(minuteElement());
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
					.append(secondElement());
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
					.append(millisElement());
			}
		};

		private final DateTimeFormatter format;
		private final int length;
		private final Period period;

		private IntervalType(DurationFieldType type) {
			this.format = configure(new DateTimeFormatterBuilder()).toFormatter();
			this.length = format.getParser().estimateParsedLength();
			this.period = new Period().withField(type, 1);
		}

		protected abstract DateTimeFormatterBuilder configure(DateTimeFormatterBuilder builder);

		public LocalInterval toInterval(String value) {
			return toInterval(format.parseLocalDateTime(value));
		}

		private LocalInterval toInterval(LocalDateTime start) {
			return new LocalInterval(start, start.plus(period));
		}

		private String toString(LocalDateTime time) {
			return format.print(time);
		}

		public List<LocalDateTime> toList(LocalInterval interval) {
			List<LocalDateTime> instants = Lists.newArrayList();
			for (LocalDateTime start = interval.getStart(); interval.contains(start); start = start.plus(period)) {
				if (instants.size() > 1440) {
					return Collections.emptyList();
				}
				instants.add(start);
			}
			return instants;
		}
	}

	private LocalIntervals() {
		throw new AssertionError();
	}

	public static LocalInterval valueOf(String value) {
		if (!value.isEmpty() && Character.isDigit(value.charAt(0))) {
			for (IntervalType format : IntervalType.values()) {
				if (value.length() == format.length) {
					return format.toInterval(value);
				}
			}
		}
		return null;
	}

	public static String toString(LocalDateTime time, String interval) {
		IntervalType format = IntervalType.valueOf(interval.toUpperCase());
		Preconditions.checkNotNull(format, "Unsupported interval: %s", interval);
		return format.toString(time);
	}

	public static List<LocalDateTime> expand(LocalDateTime start, LocalDateTime end, String interval) {
		IntervalType format = IntervalType.valueOf(interval.toUpperCase());
		Preconditions.checkNotNull(format, "Unsupported interval: %s", interval);
		return format.toList(new LocalInterval(start, end));
	}
}
