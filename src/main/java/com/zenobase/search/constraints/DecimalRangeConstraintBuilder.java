package com.zenobase.search.constraints;

import com.google.common.collect.Range;
import com.zenobase.common.DecimalRangeParser;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

public class DecimalRangeConstraintBuilder extends RangeConstraintBuilderSupport<BigDecimal> {

	private final DecimalRangeParser parser = new DecimalRangeParser();

	public DecimalRangeConstraintBuilder(String path) {
		super(path);
	}

	@Override
	protected @Nullable Range<BigDecimal> parseRange(String value) {
		return parser.parse(value);
	}

	@Override
	protected Number getValue(BigDecimal value) {
		return value;
	}
}
