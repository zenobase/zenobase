package com.zenobase.common;

import org.joda.time.ReadableInstant;

public class OffsetDateTimeRangeParser extends RangeParser<ReadableInstant> {

	@Override
	protected ReadableInstant getValue(String s) {
		return OffsetDateTimeFormat.hasOffset(s) ? OffsetDateTimeFormat.parse(s) : null;
	}
}
