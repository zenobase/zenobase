package com.zenobase.search;

import java.math.BigDecimal;

public class PhaseRangeConstraintBuilder extends DecimalRangeConstraintBuilder {

	public PhaseRangeConstraintBuilder(String path) {
		super(path);
	}

	@Override
	protected Number getValue(BigDecimal value) {
		return value.remainder(BigDecimal.ONE);
	}
}
