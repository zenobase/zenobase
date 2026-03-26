package com.zenobase.json;

import org.junit.jupiter.api.Test;

public class BooleanFieldTest extends FieldTestSupport<Boolean> {

	@Override
	protected Field<Boolean> newField(String name) {
		return new BooleanField(name);
	}

	@Test
	public void test() {
		roundtrip(Boolean.TRUE);
		roundtrip(Boolean.FALSE);
	}
}
