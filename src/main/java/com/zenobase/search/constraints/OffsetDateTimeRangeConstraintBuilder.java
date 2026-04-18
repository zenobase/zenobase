package com.zenobase.search.constraints;

import com.google.common.collect.Range;
import com.zenobase.common.OffsetDateTimeRangeParser;
import org.joda.time.ReadableInstant;
import org.jspecify.annotations.Nullable;

public class OffsetDateTimeRangeConstraintBuilder extends RangeConstraintBuilderSupport<ReadableInstant> {

	private final OffsetDateTimeRangeParser parser = new OffsetDateTimeRangeParser();

	public OffsetDateTimeRangeConstraintBuilder(String path) {
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
