package com.zenobase.common;

import java.util.Objects;

import com.google.common.base.Preconditions;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.format.ISODateTimeFormat;

/**
 * Interval without a time zone.
 */
public class LocalInterval implements Comparable<LocalInterval> {

	private final LocalDateTime start, end;

	public LocalInterval(LocalDateTime start, LocalDateTime end) {
		Preconditions.checkArgument(!end.isBefore(start), "The end instant must be greater or equal to the start");
		this.start = start;
		this.end = end;
	}

	public LocalDateTime getStart() {
		return start;
	}

	public LocalDateTime getEnd() {
		return end;
	}

	public boolean contains(LocalDateTime time) {
		return !start.isAfter(time) && end.isAfter(time);
	}

	public long toDurationMillis() {
		return end.toDateTime(DateTimeZone.UTC).getMillis()
				- start.toDateTime(DateTimeZone.UTC).getMillis();
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
	public boolean equals(Object that) {
		return that instanceof LocalInterval i && start.equals(i.start) && end.equals(i.end);
	}

	@Override
	public int hashCode() {
		return Objects.hash(start, end);
	}

	@Override
	public String toString() {
		var printer = ISODateTimeFormat.dateTime();
		var sb = new StringBuilder(48);
		printer.printTo(sb, getStart());
		sb.append('/');
		printer.printTo(sb, getEnd());
		return sb.toString();
	}
}
