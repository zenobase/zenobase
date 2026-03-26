package com.zenobase.common;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import org.joda.time.ReadableInstant;
import org.jspecify.annotations.Nullable;

public class OffsetDateTimeRangeParser extends RangeParser<ReadableInstant> {

	private final OffsetIntervalRangeParser parser = new OffsetIntervalRangeParser();

	@Override
	public @Nullable Range<ReadableInstant> parse(String value) {
		Range<ComparableInterval> range = parser.parse(value);
		return range != null ? toRange(range) : null;
	}

	private static Range<ReadableInstant> toRange(Range<ComparableInterval> range) {
		if (!range.hasLowerBound()) {
			return Range.lessThan(getUpper(range));
		} else if (!range.hasUpperBound()) {
			return Range.atLeast(getLower(range));
		} else {
			return Range.closedOpen(getLower(range), getUpper(range));
		}
	}

	private static ReadableInstant getLower(Range<ComparableInterval> range) {
		return range.lowerBoundType() == BoundType.CLOSED
				? range.lowerEndpoint().getStart()
				: range.lowerEndpoint().getEnd();
	}

	private static ReadableInstant getUpper(Range<ComparableInterval> range) {
		return range.upperBoundType() == BoundType.OPEN
				? range.upperEndpoint().getStart()
				: range.upperEndpoint().getEnd();
	}

	@Override
	protected ReadableInstant getValue(String s) {
		throw new UnsupportedOperationException();
	}
}
