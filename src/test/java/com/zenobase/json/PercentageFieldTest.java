package com.zenobase.json;

import com.zenobase.models.Percentage;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

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
