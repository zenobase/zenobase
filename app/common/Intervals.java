package common;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import org.joda.time.Years;

public class Intervals {

	private Intervals() {
		
	}

	public static Interval forYear(Years year, DateTimeZone tz) {
		return new Interval(toDateTime(year, tz), toDateTime(year.plus(1), tz));
	}

	private static DateTime toDateTime(Years year, DateTimeZone tz) {
		return new DateTime(year.getYears(), 1, 1, 0, 0, tz);
	}

	public static Interval forMonth(YearMonth month, DateTimeZone tz) {
		return new Interval(toDateTime(month, tz), toDateTime(month.plusMonths(1), tz));
	}

	private static DateTime toDateTime(YearMonth month, DateTimeZone tz) {
		return new DateTime(month.getYear(), month.getMonthOfYear(), 1, 0, 0, tz);
	}

	public static Interval forDay(LocalDate day, DateTimeZone tz) {
		return new Interval(toDateTime(day, tz), toDateTime(day.plusDays(1), tz));
	}

	private static DateTime toDateTime(LocalDate day, DateTimeZone tz) {
		return new DateTime(day.getYear(), day.getMonthOfYear(), day.getDayOfMonth(), 0, 0, tz);
	}
}
