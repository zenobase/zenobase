package com.zenobase.search;

import org.joda.time.ReadableInstant;
import com.google.common.collect.Range;

import com.zenobase.common.EpochDateTimeRangeParser;
import com.zenobase.json.Field;

public class EpochDateTimeRangeConstraintBuilder extends RangeConstraintBuilderSupport<ReadableInstant> {

	private final EpochDateTimeRangeParser parser = new EpochDateTimeRangeParser();

	public EpochDateTimeRangeConstraintBuilder(Field<?> field) {
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
