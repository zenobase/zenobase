package common;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public enum Intervals {

	YEAR(ISODateTimeFormat.year()),
	MONTH(ISODateTimeFormat.yearMonth()),
	DAY(ISODateTimeFormat.date()),
	HOUR(ISODateTimeFormat.dateHour()),
	MINUTE(ISODateTimeFormat.dateHourMinute());

	private final DateTimeFormatter format;

	private Intervals(DateTimeFormatter format) {
		this.format = format;
	}

	public DateTime parse(String value, DateTimeZone tz) {
		return format.withZone(tz).parseDateTime(value);
	}

	public String toString(DateTime time) {
		return format.print(time);
	}

	public static Interval valueOf(String value, DateTimeZone tz) {
		if (value.length() == 4) {
			return forYear(YEAR.parse(value, tz));
		}
		if (value.length() == 7) {
			return forMonth(MONTH.parse(value, tz));
		}
		if (value.length() == 10) {
			return forDay(DAY.parse(value, tz));
		}
		if (value.length() == 13) {
			return forHour(HOUR.parse(value, tz));
		}
		if (value.length() == 16) {
			return forMinute(MINUTE.parse(value, tz));
		}
		throw new IllegalArgumentException("Unsupported date/time format: " + value);
	}

	private static Interval forYear(DateTime year) {
		return new Interval(year, year.plusYears(1));
	}

	private static Interval forMonth(DateTime month) {
		return new Interval(month, month.plusMonths(1));
	}

	private static Interval forDay(DateTime day) {
		return new Interval(day, day.plusDays(1));
	}

	private static Interval forHour(DateTime hour) {
		return new Interval(hour, hour.plusHours(1));
	}

	private static Interval forMinute(DateTime minute) {
		return new Interval(minute, minute.plusMinutes(1));
	}
}
