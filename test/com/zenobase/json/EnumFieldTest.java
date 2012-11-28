package com.zenobase.json;

import org.junit.Test;

public class EnumFieldTest extends FieldTestSupport {

	private enum Option {
		A, B, C
	}

	@Test
	public void test() {
		roundtrip(EnumField.newInstance(FIELD_NAME, Option.class), Option.A);
	}
}
