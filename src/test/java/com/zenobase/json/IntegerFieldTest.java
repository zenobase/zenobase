package com.zenobase.json;

import org.junit.jupiter.api.Test;

public class IntegerFieldTest extends FieldTestSupport<Integer> {

	@Override
	protected Field<Integer> newField(String name) {
		return new IntegerField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(0);
		roundtrip(42);
		roundtrip(Integer.MIN_VALUE);
		roundtrip(Integer.MAX_VALUE);
	}
}
