package com.zenobase.common;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

public class CustomDateTimeFormat {

	/**
     * Returns an ISO datetime formatter where the year and timezone offset are mandatory,
     * and the rest is optional:
     * <code>yyyy ['-' MM ['-' dd ['T' [HH [':' mm [':' ss ['.' SSS]]]]]]] Z</code>
     */
	public static DateTimeFormatter format() {
		return new DateTimeFormatterBuilder()
			.append(yearElement())
			.appendOptional(monthElement().getParser())
			.appendOptional(dayOfMonthElement().getParser())
			.appendLiteral('T')
			.appendOptional(hourElement().getParser())
			.appendOptional(minuteElement().getParser())
			.appendOptional(secondElement().getParser())
			.appendOptional(millisElement().getParser())
			.append(offsetElement())
			.toFormatter()
			.withOffsetParsed();
	}

	public static DateTimeFormatter inclYear() {
		return new DateTimeFormatterBuilder()
			.append(yearElement())
			.appendLiteral('T')
			.append(offsetElement())
			.toFormatter()
			.withOffsetParsed();
	}

	public static DateTimeFormatter inclMonth() {
		return new DateTimeFormatterBuilder()
			.append(yearElement())
			.append(monthElement())
			.appendLiteral('T')
			.append(offsetElement())
			.toFormatter()
			.withOffsetParsed();
	}

	public static DateTimeFormatter inclDayOfMonth() {
		return new DateTimeFormatterBuilder()
			.append(yearElement())
			.append(monthElement())
			.append(dayOfMonthElement())
			.appendLiteral('T')
			.append(offsetElement())
			.toFormatter()
			.withOffsetParsed();
	}

	public static DateTimeFormatter inclHour() {
		return new DateTimeFormatterBuilder()
			.append(yearElement())
			.append(monthElement())
			.append(dayOfMonthElement())
			.appendLiteral('T')
			.append(hourElement())
			.append(offsetElement())
			.toFormatter()
			.withOffsetParsed();
	}

	public static DateTimeFormatter inclMinute() {
		return new DateTimeFormatterBuilder()
			.append(yearElement())
			.append(monthElement())
			.append(dayOfMonthElement())
			.appendLiteral('T')
			.append(hourElement())
			.append(minuteElement())
			.append(offsetElement())
			.toFormatter()
			.withOffsetParsed();
	}

	public static DateTimeFormatter inclSecond() {
		return new DateTimeFormatterBuilder()
			.append(yearElement())
			.append(monthElement())
			.append(dayOfMonthElement())
			.appendLiteral('T')
			.append(hourElement())
			.append(minuteElement())
			.append(secondElement())
			.append(offsetElement())
			.toFormatter()
			.withOffsetParsed();
	}

	public static DateTimeFormatter inclMillis() {
		return new DateTimeFormatterBuilder()
			.append(yearElement())
			.append(monthElement())
			.append(dayOfMonthElement())
			.appendLiteral('T')
			.append(hourElement())
			.append(minuteElement())
			.append(secondElement())
			.append(millisElement())
			.append(offsetElement())
			.toFormatter()
			.withOffsetParsed();
	}

	private static DateTimeFormatter yearElement() {
		return new DateTimeFormatterBuilder()
			.appendYear(4, 4)
			.toFormatter();
	}

	private static DateTimeFormatter monthElement() {
		return new DateTimeFormatterBuilder()
			.appendLiteral('-')
			.appendMonthOfYear(2)
			.toFormatter();
	}

	private static DateTimeFormatter dayOfMonthElement() {
		return new DateTimeFormatterBuilder()
			.appendLiteral('-')
			.appendDayOfMonth(2)
			.toFormatter();
	}

	private static DateTimeFormatter hourElement() {
		return new DateTimeFormatterBuilder()
			.appendHourOfDay(2)
			.toFormatter();
	}

	private static DateTimeFormatter minuteElement() {
		return new DateTimeFormatterBuilder()
			.appendLiteral(':')
			.appendMinuteOfHour(2)
			.toFormatter();
	}

	private static DateTimeFormatter secondElement() {
		return new DateTimeFormatterBuilder()
			.appendLiteral(':')
			.appendSecondOfMinute(2)
			.toFormatter();
	}

	private static DateTimeFormatter millisElement() {
		return new DateTimeFormatterBuilder()
			.appendLiteral('.')
			.appendFractionOfSecond(3, 3)
			.toFormatter();
	}

	private static DateTimeFormatter offsetElement() {
		return new DateTimeFormatterBuilder()
			.appendTimeZoneOffset("Z", true, 2, 2)
			.toFormatter();
	}
}
