package com.zenobase.json;

import com.zenobase.json.EnumField;

import org.junit.Test;

public class EnumFieldTest extends FieldTestSupport {

	private enum Option {
		A, B, C
	}

	@Test
	public void test() {
		roundtrip(new EnumField<Option>(FIELD_NAME, Option.class), Option.A);
	}
}
