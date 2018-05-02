package com.zenobase.common;

import org.joda.time.Interval;
import org.joda.time.ReadableInterval;
import org.joda.time.base.BaseInterval;

/**
 * Comparable Interval that can be used in a Range.
 */
public class ComparableInterval extends BaseInterval implements ReadableInterval, Comparable<ComparableInterval> {

	private static final long serialVersionUID = -5270005086692266606L;

	public ComparableInterval(Interval interval) {
		super(interval.getStart(), interval.getEnd());
	}

	@Override
	public int compareTo(ComparableInterval that) {
		if (equals(that)) {
			return 0;
		}
		if (isAfter(that)) {
			return 1;
		}
		return -1;
	}
}
