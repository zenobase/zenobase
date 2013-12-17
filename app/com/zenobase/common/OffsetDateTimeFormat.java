package com.zenobase.common;

import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

/**
 * ISO date time format where the year and timezone offset are mandatory,
 * and the rest is optional:
 * <code>yyyy ['-' MM ['-' dd ['T' [HH [':' mm [':' ss ['.' SSS]]]]]]] Z</code>
 */
public class OffsetDateTimeFormat extends DateTimeFormatSupport {

	private static final DateTimeFormatter PARSER = new DateTimeFormatterBuilder()
		.append(yearElement())
		.appendOptional(monthElement().getParser())
		.appendOptional(dayOfMonthElement().getParser())
		.appendOptional(weekofYearElement().getParser())
		.appendLiteral('T')
		.appendOptional(hourElement().getParser())
		.appendOptional(minuteElement().getParser())
		.appendOptional(secondElement().getParser())
		.appendOptional(millisElement().getParser())
		.append(offsetElement())
		.toFormatter()
		.withOffsetParsed();

	public static DateTime parse(String s) {
		return PARSER.parseDateTime(s);
	}

	private static final Pattern TIMEZONE_OFFSET = Pattern.compile("Z|[+-]\\d\\d:\\d\\d");

	public static boolean hasOffset(String s) {
		return TIMEZONE_OFFSET.matcher(s).find();
	}
}
