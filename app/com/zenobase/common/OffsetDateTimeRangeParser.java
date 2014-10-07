package com.zenobase.common;

import org.joda.time.ReadableInstant;
import com.google.common.collect.BoundType;
import com.google.common.collect.Range;

public class OffsetDateTimeRangeParser extends RangeParser<ReadableInstant> {

	private final OffsetIntervalRangeParser parser = new OffsetIntervalRangeParser();

	@Override
	public Range<ReadableInstant> parse(String value) {
		Range<ComparableInterval> range = parser.parse(value);
		return range != null ? toRange(range) : null;
	}

	private static Range<ReadableInstant> toRange(Range<ComparableInterval> range) {
		ReadableInstant lower = range.lowerBoundType() == BoundType.CLOSED ? range.lowerEndpoint().getStart() : range.lowerEndpoint().getEnd();
		ReadableInstant upper = range.upperBoundType() == BoundType.OPEN ? range.upperEndpoint().getStart() : range.upperEndpoint().getEnd();
		if (!range.hasLowerBound()) {
			return Range.lessThan(upper);
		} else if (!range.hasUpperBound()) {
			return Range.atLeast(lower);
		} else {
			return Range.closedOpen(lower, upper);
		}
	}

	@Override
	protected ReadableInstant getValue(String s) {
		throw new UnsupportedOperationException();
	}
}
