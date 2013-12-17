package com.zenobase.common;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

public class DateTimeFormatSupport {

	protected static DateTimeFormatter yearElement() {
		return new DateTimeFormatterBuilder()
			.appendYear(4, 4)
			.toFormatter();
	}

	protected static DateTimeFormatter monthElement() {
		return new DateTimeFormatterBuilder()
			.appendLiteral('-')
			.appendMonthOfYear(2)
			.toFormatter();
	}

	protected static DateTimeFormatter weekofYearElement() {
		return new DateTimeFormatterBuilder()
			.appendLiteral('-')
			.appendLiteral('W')
			.appendWeekOfWeekyear(2)
			.toFormatter();
	}

	protected static DateTimeFormatter dayOfMonthElement() {
		return new DateTimeFormatterBuilder()
			.appendLiteral('-')
			.appendDayOfMonth(2)
			.toFormatter();
	}

	protected static DateTimeFormatter tElement() {
		return new DateTimeFormatterBuilder()
			.appendLiteral('T')
			.toFormatter();
	}

	protected static DateTimeFormatter hourElement() {
		return new DateTimeFormatterBuilder()
			.appendHourOfDay(2)
			.toFormatter();
	}

	protected static DateTimeFormatter minuteElement() {
		return new DateTimeFormatterBuilder()
			.appendLiteral(':')
			.appendMinuteOfHour(2)
			.toFormatter();
	}

	protected static DateTimeFormatter secondElement() {
		return new DateTimeFormatterBuilder()
			.appendLiteral(':')
			.appendSecondOfMinute(2)
			.toFormatter();
	}

	protected static DateTimeFormatter millisElement() {
		return new DateTimeFormatterBuilder()
			.appendLiteral('.')
			.appendFractionOfSecond(3, 3)
			.toFormatter();
	}

	protected static DateTimeFormatter offsetElement() {
		return new DateTimeFormatterBuilder()
			.appendTimeZoneOffset("Z", true, 2, 2)
			.toFormatter();
	}
}
