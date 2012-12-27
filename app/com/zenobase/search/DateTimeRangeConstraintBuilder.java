package com.zenobase.search;

import org.joda.time.ReadableInstant;
import com.google.common.collect.Range;

import com.zenobase.common.DateTimeRangeParser;

public class DateTimeRangeConstraintBuilder extends RangeConstraintBuilderSupport<ReadableInstant> {

	private final DateTimeRangeParser parser = new DateTimeRangeParser();

	@Override
	protected Range<ReadableInstant> parseRange(String value) {
		return parser.parse(value);
	}

	@Override
	protected Number getValue(ReadableInstant value) {
		return value.getMillis();
	}
}
