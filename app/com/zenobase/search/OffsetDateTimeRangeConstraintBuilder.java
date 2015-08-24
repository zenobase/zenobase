package com.zenobase.search;

import com.google.common.collect.Range;
import org.joda.time.ReadableInstant;

import com.zenobase.common.OffsetDateTimeRangeParser;

public class OffsetDateTimeRangeConstraintBuilder extends RangeConstraintBuilderSupport<ReadableInstant> {

	private final OffsetDateTimeRangeParser parser = new OffsetDateTimeRangeParser();

	public OffsetDateTimeRangeConstraintBuilder(String path) {
		super(path);
	}

	@Override
	protected Range<ReadableInstant> parseRange(String value) {
		return parser.parse(value);
	}

	@Override
	protected Number getValue(ReadableInstant value) {
		return value.getMillis();
	}
}
