package com.zenobase.search;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.ReadablePartial;
import com.google.common.collect.Range;

import com.zenobase.common.LocalDateTimeRangeParser;

public class LocalDateTimeRangeConstraintBuilder extends RangeConstraintBuilderSupport<ReadablePartial> {

	private final LocalDateTimeRangeParser parser = new LocalDateTimeRangeParser();

	public LocalDateTimeRangeConstraintBuilder(String path) {
		super(path);
	}

	@Override
	protected Range<ReadablePartial> parseRange(String value) {
		return parser.parse(value);
	}

	@Override
	protected Number getValue(ReadablePartial value) {
		return value.toDateTime(DateTime.now(DateTimeZone.UTC)).getMillis();
	}
}
