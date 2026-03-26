package com.zenobase.common;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import org.joda.time.ReadablePartial;

public class LocalDateTimeRangeParser extends RangeParser<ReadablePartial> {

	private final LocalIntervalRangeParser parser = new LocalIntervalRangeParser();

	@Override
	public Range<ReadablePartial> parse(String value) {
		Range<LocalInterval> range = parser.parse(value);
		return range != null ? toRange(range) : null;
	}

	private static Range<ReadablePartial> toRange(Range<LocalInterval> range) {
		if (!range.hasLowerBound()) {
			return Range.lessThan(getUpper(range));
		} else if (!range.hasUpperBound()) {
			return Range.atLeast(getLower(range));
		} else {
			return Range.closedOpen(getLower(range), getUpper(range));
		}
	}

	private static ReadablePartial getLower(Range<LocalInterval> range) {
		return range.lowerBoundType() == BoundType.CLOSED
				? range.lowerEndpoint().getStart()
				: range.lowerEndpoint().getEnd();
	}

	private static ReadablePartial getUpper(Range<LocalInterval> range) {
		return range.upperBoundType() == BoundType.OPEN
				? range.upperEndpoint().getStart()
				: range.upperEndpoint().getEnd();
	}

	@Override
	protected ReadablePartial getValue(String s) {
		throw new UnsupportedOperationException();
	}
}
