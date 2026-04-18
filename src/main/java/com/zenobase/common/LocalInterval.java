package com.zenobase.common;

import com.google.common.base.Preconditions;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Interval without a time zone.
 */
public record LocalInterval(LocalDateTime start, LocalDateTime end) implements Comparable<LocalInterval> {
	public LocalInterval {
		Preconditions.checkArgument(!end.isBefore(start), "The end instant must be greater or equal to the start");
	}

	public boolean contains(LocalDateTime time) {
		return !start.isAfter(time) && end.isAfter(time);
	}

	public long toDurationMillis() {
		return end.toDateTime(DateTimeZone.UTC).getMillis() - start.toDateTime(DateTimeZone.UTC).getMillis();
	}

	@Override
	public int compareTo(LocalInterval that) {
		if (equals(that)) {
			return 0;
		}
		int cmp = start.compareTo(that.start);
		return cmp != 0 ? cmp : end.compareTo(that.end);
	}

	@Override
	public String toString() {
		var printer = ISODateTimeFormat.dateTime();
		var sb = new StringBuilder(48);
		printer.printTo(sb, start());
		sb.append('/');
		printer.printTo(sb, end());
		return sb.toString();
	}
}
