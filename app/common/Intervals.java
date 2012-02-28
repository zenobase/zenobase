package common;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Interval;
import org.joda.time.Minutes;
import org.joda.time.Months;
import org.joda.time.ReadablePeriod;
import org.joda.time.Years;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class Intervals {

	private enum Format {

		YEAR(ISODateTimeFormat.year(), 4, Years.years(1)),
		MONTH(ISODateTimeFormat.yearMonth(), 7, Months.months(1)),
		DAY(ISODateTimeFormat.date(), 10, Days.days(1)),
		HOUR(ISODateTimeFormat.dateHour(), 13, Hours.hours(1)),
		MINUTE(ISODateTimeFormat.dateHourMinute(), 16, Minutes.minutes(1));
	
		private final DateTimeFormatter format;
		private final int length;
		private final ReadablePeriod unit;
	
		private Format(DateTimeFormatter format, int length, ReadablePeriod unit) {
			this.format = format;
			this.length = length;
			this.unit = unit;
		}

		public DateTime parse(String value, DateTimeZone tz) {
			return format.withZone(tz).parseDateTime(value);
		}

		public Interval toInterval(DateTime start) {
			return new Interval(start, start.plus(unit));
		}

		public Interval toInterval(String value, DateTimeZone tz) {
			return toInterval(parse(value, tz));
		}

		public String toString(DateTime time) {
			return format.print(time);
		}

		public List<DateTime> expand(Interval interval) {
			List<DateTime> times = Lists.newArrayList();
			for (DateTime start = interval.getStart(); interval.contains(start); start = start.plus(unit)) {
				times.add(start);
			}
			return times;
		}	
	}

	public static Interval valueOf(String value, DateTimeZone tz) {
		for (Format format : Format.values()) {
			if (value.length() == format.length) {
				return format.toInterval(value, tz);
			}
		}
		throw new IllegalArgumentException("Unsupported date/time format: " + value);
	}

	public static String toString(DateTime time, String interval) {
		Format format = Format.valueOf(interval.toUpperCase());
		Preconditions.checkNotNull(format, "Unsupported interval: %s", interval);
		return format.toString(time);
	}

	public static List<DateTime> expand(DateTime start, DateTime end, String interval) {
		Format format = Format.valueOf(interval.toUpperCase());
		Preconditions.checkNotNull(format, "Unsupported interval: %s", interval);
		return format.expand(new Interval(start, end));
	}
}
