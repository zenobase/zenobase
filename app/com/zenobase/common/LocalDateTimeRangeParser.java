package com.zenobase.common;

import org.joda.time.ReadablePartial;
import com.google.common.collect.BoundType;
import com.google.common.collect.Range;

public class LocalDateTimeRangeParser extends RangeParser<ReadablePartial> {

	private final LocalIntervalRangeParser parser = new LocalIntervalRangeParser();

	@Override
	public Range<ReadablePartial> parse(String value) {
		Range<LocalInterval> range = parser.parse(value);
		return range != null ? toRange(range) : null;
	}

	private static Range<ReadablePartial> toRange(Range<LocalInterval> range) {
		ReadablePartial lower = range.lowerBoundType() == BoundType.CLOSED
			? range.lowerEndpoint().getStart()
			: range.lowerEndpoint().getEnd();
		ReadablePartial upper = range.upperBoundType() == BoundType.OPEN
			? range.upperEndpoint().getStart()
			: range.upperEndpoint().getEnd();
		if (!range.hasLowerBound()) {
			return Range.lessThan(upper);
		} else if (!range.hasUpperBound()) {
			return Range.atLeast(lower);
		} else {
			return Range.closedOpen(lower, upper);
		}
	}

	@Override
	protected ReadablePartial getValue(String s) {
		throw new UnsupportedOperationException();
	}
}
