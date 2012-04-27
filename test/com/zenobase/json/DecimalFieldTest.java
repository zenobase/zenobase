package com.zenobase.json;

import java.math.BigDecimal;

import com.zenobase.json.DecimalField;

import org.junit.Test;

public class DecimalFieldTest extends FieldTestSupport {

	private final DecimalField field = new DecimalField(FIELD_NAME);

	@Test
	public void test() {
		roundtrip(field, BigDecimal.valueOf(42));
		roundtrip(field, BigDecimal.ZERO);
		roundtrip(field, new BigDecimal("1.234567890"));
	}
}
