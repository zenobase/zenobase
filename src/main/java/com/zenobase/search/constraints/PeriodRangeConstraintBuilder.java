package com.zenobase.search.constraints;

import com.google.common.collect.Range;
import com.zenobase.common.PeriodRangeParser;
import com.zenobase.common.StandardPeriod;
import org.jspecify.annotations.Nullable;

public class PeriodRangeConstraintBuilder extends RangeConstraintBuilderSupport<StandardPeriod> {

	private final PeriodRangeParser parser = new PeriodRangeParser();

	public PeriodRangeConstraintBuilder(String path) {
		super(path);
	}

	@Override
	protected @Nullable Range<StandardPeriod> parseRange(String value) {
		return parser.parse(value);
	}

	@Override
	protected String getValue(StandardPeriod value) {
		return "now" + value;
	}
}
