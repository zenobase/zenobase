package com.zenobase.json;

import java.math.BigDecimal;

import org.junit.Test;

import com.zenobase.models.Percentage;

public class PercentageFieldTest extends FieldTestSupport<Percentage> {

	@Override
	protected Field<Percentage> newField(String name) {
		return new PercentageField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(valueOf("0"));
		roundtrip(valueOf("1.23456789"));
		roundtrip(valueOf("100"));
	}

	private static Percentage valueOf(String s) {
		return Percentage.valueOf(new BigDecimal(s));
	}
}
