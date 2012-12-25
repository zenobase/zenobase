package com.zenobase.common;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;

public class CustomDateTimeFormat {

	private static final DateTimeFormatter FORMAT = new DateTimeFormatterBuilder()
		.append(yearElement())
		.appendOptional(monthElement())
		.appendOptional(dayOfMonthElement())
		.appendLiteral('T')
		.appendOptional(hourElement())
		.appendOptional(minuteElement())
		.appendOptional(secondElement())
		.appendOptional(fractionElement())
		.append(offsetElement())
		.toFormatter()
		.withOffsetParsed();

	private static DateTimeParser yearElement() {
		return new DateTimeFormatterBuilder()
			.appendYear(4, 9)
			.toParser();
	}

	private static DateTimeParser monthElement() {
		return new DateTimeFormatterBuilder()
			.appendLiteral('-')
			.appendMonthOfYear(2)
			.toParser();
	}

	private static DateTimeParser dayOfMonthElement() {
		return new DateTimeFormatterBuilder()
			.appendLiteral('-')
			.appendDayOfMonth(2)
			.toParser();
	}

	private static DateTimeParser hourElement() {
		return new DateTimeFormatterBuilder()
			.appendHourOfDay(2)
			.toParser();
	}

	private static DateTimeParser minuteElement() {
		return new DateTimeFormatterBuilder()
			.appendLiteral(':')
			.appendMinuteOfHour(2)
			.toParser();
	}

	private static DateTimeParser secondElement() {
		return new DateTimeFormatterBuilder()
			.appendLiteral(':')
			.appendSecondOfMinute(2)
			.toParser();
	}

	private static DateTimeParser fractionElement() {
		return new DateTimeFormatterBuilder()
			.appendLiteral('.')
			.appendFractionOfSecond(3, 3)
			.toParser();
	}

	private static DateTimeParser offsetElement() {
		return new DateTimeFormatterBuilder()
			.appendTimeZoneOffset("Z", true, 2, 4)
			.toParser();
	}

	/**
     * Returns an ISO datetime formatter where the year and timezone offset are mandatory,
     * and the rest is optional:
     * <code>yyyy ['-' MM ['-' dd ['T' [HH [':' mm [':' ss ['.' SSS]]]]]]] Z</code>
     */
	public static DateTimeFormatter format() {
		return FORMAT;
	}
}
