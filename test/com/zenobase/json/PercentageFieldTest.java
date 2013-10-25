package com.zenobase.json;

import java.math.BigDecimal;

import org.junit.Test;

import com.zenobase.models.Percentage;

public class PercentageFieldTest extends FieldTestSupport {

	private final PercentageField field = new PercentageField(FIELD_NAME);

	@Test
	public void test() {
		roundtrip(field, Percentage.valueOf(new BigDecimal("0")));
		roundtrip(field, Percentage.valueOf(new BigDecimal("1.23456789")));
		roundtrip(field, Percentage.valueOf(new BigDecimal("100")));
	}
}
