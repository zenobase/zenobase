package com.zenobase.common;

import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DurationFieldType;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import com.google.common.collect.ImmutableMap;

public class CustomDateTimeFormat extends DateTimeFormatSupport {

	private static final DateTimeFormatter PARSER = new DateTimeFormatterBuilder()
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

	/**
	 * Returns an ISO datetime formatter where the year and timezone offset are mandatory,
	 * and the rest is optional:
	 * <code>yyyy ['-' MM ['-' dd ['T' [HH [':' mm [':' ss ['.' SSS]]]]]]] Z</code>
	 */
	public static DateTime parse(String s) {
		return PARSER.parseDateTime(s);
	}

	public static DateTimeFormatter year() {
		return new DateTimeFormatterBuilder()
			.append(yearElement())
			.appendLiteral('T')
			.append(offsetElement())
			.toFormatter()
			.withOffsetParsed();
	}

	public static DateTimeFormatter month() {
		return new DateTimeFormatterBuilder()
			.append(yearElement())
			.append(monthElement())
			.appendLiteral('T')
			.append(offsetElement())
			.toFormatter()
			.withOffsetParsed();
	}

	public static DateTimeFormatter day() {
		return new DateTimeFormatterBuilder()
			.append(yearElement())
			.append(monthElement())
			.append(dayOfMonthElement())
			.appendLiteral('T')
			.append(offsetElement())
			.toFormatter()
			.withOffsetParsed();
	}

	public static DateTimeFormatter hour() {
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

	public static DateTimeFormatter minute() {
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

	public static DateTimeFormatter second() {
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

	public static DateTimeFormatter millis() {
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

	private static final Map<DurationFieldType, DateTimeFormatter> FORMATS =
		ImmutableMap.<DurationFieldType, DateTimeFormatter>builder()
			.put(DurationFieldType.years(), year())
			.put(DurationFieldType.months(), month())
			.put(DurationFieldType.days(), day())
			.put(DurationFieldType.hours(), hour())
			.put(DurationFieldType.minutes(), minute())
			.put(DurationFieldType.seconds(), second())
			.put(DurationFieldType.millis(), millis())
			.build();

	public static DateTimeFormatter format(DurationFieldType type) {
		return FORMATS.get(type);
	}
}
