package com.zenobase.search.constraints;

import com.google.common.collect.Range;
import org.joda.time.ReadableInstant;
import org.jspecify.annotations.Nullable;

import com.zenobase.common.EpochDateTimeRangeParser;

public class EpochDateTimeRangeConstraintBuilder extends RangeConstraintBuilderSupport<ReadableInstant> {

	private final EpochDateTimeRangeParser parser = new EpochDateTimeRangeParser();

	public EpochDateTimeRangeConstraintBuilder(String path) {
		super(path);
	}

	@Override
	protected @Nullable Range<ReadableInstant> parseRange(String value) {
		return parser.parse(value);
	}

	@Override
	protected Number getValue(ReadableInstant value) {
		return value.getMillis();
	}
}
