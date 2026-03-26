package com.zenobase.search;

import com.google.common.collect.Range;
import org.joda.time.ReadableDuration;
import org.jspecify.annotations.Nullable;

import com.zenobase.common.DurationRangeParser;

public class DurationRangeConstraintBuilder extends RangeConstraintBuilderSupport<ReadableDuration> {

	private final DurationRangeParser parser = new DurationRangeParser();

	public DurationRangeConstraintBuilder(String path) {
		super(path);
	}

	@Override
	protected @Nullable Range<ReadableDuration> parseRange(String value) {
		return parser.parse(value);
	}

	@Override
	protected Long getValue(ReadableDuration value) {
		return value.getMillis();
	}
}
