package com.zenobase.common;

import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

/**
 * ISO local date time format where only the year is mandatory:
 * <code>yyyy ['-' MM ['-' dd ['T' [HH [':' mm [':' ss ['.' SSS]]]]]]]</code>
 */
public class LocalDateTimeFormat extends DateTimeFormatSupport {

	private static final DateTimeFormatter PARSER = new DateTimeFormatterBuilder()
		.append(yearElement())
		.appendOptional(monthElement().getParser())
		.appendOptional(dayOfMonthElement().getParser())
		.appendOptional(weekofYearElement().getParser())
		.appendOptional(tElement().getParser())
		.appendOptional(hourElement().getParser())
		.appendOptional(minuteElement().getParser())
		.appendOptional(secondElement().getParser())
		.appendOptional(millisElement().getParser())
		.toFormatter();

	public static LocalDateTime parse(String s) {
		return PARSER.parseLocalDateTime(s);
	}
}
