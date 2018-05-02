package com.zenobase.common;

import java.math.BigDecimal;

public class DecimalRangeParser extends RangeParser<BigDecimal> {

	@Override
	protected BigDecimal getValue(String s) {
		return new BigDecimal(s);
	}
}
