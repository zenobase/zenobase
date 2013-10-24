package com.zenobase.search;

import org.joda.time.ReadableInstant;
import com.google.common.collect.Range;

import com.zenobase.common.OffsetDateTimeRangeParser;
import com.zenobase.json.Field;

public class OffsetDateTimeRangeConstraintBuilder extends RangeConstraintBuilderSupport<ReadableInstant> {

	private final OffsetDateTimeRangeParser parser = new OffsetDateTimeRangeParser();

	public OffsetDateTimeRangeConstraintBuilder(Field<?> field) {
		super(field);
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
