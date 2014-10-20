package com.zenobase.json;

import org.junit.Test;

public class EnumFieldTest extends FieldTestSupport<TestEnum> {

	@Override
	protected Field<TestEnum> newField(String name) {
		return EnumField.newInstance(name, TestEnum.class);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(TestEnum.A);
	}
}
