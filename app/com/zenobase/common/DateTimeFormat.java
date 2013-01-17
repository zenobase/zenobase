package com.zenobase.common;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

/**
 * ISO date time format where the year and timezone offset are mandatory,
 * and the rest is optional:
 * <code>yyyy ['-' MM ['-' dd ['T' [HH [':' mm [':' ss ['.' SSS]]]]]]] Z</code>
 */
public class DateTimeFormat extends DateTimeFormatSupport {

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

	public static DateTime parse(String s) {
		return PARSER.parseDateTime(s);
	}
}
