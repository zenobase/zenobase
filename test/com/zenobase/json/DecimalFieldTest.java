package com.zenobase.json;

import java.math.BigDecimal;

import org.junit.Test;

public class DecimalFieldTest extends FieldTestSupport<BigDecimal> {

	@Override
	protected Field<BigDecimal> newField(String name) {
		return new DecimalField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(BigDecimal.valueOf(42));
		roundtrip(BigDecimal.ZERO);
		roundtrip(BigDecimal.ONE);
		roundtrip(new BigDecimal("1.0"));
		roundtrip(new BigDecimal("1.2345678901234567"));
	}
}
