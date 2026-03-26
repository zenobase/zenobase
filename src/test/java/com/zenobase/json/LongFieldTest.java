package com.zenobase.json;

import org.junit.jupiter.api.Test;

public class LongFieldTest extends FieldTestSupport<Long> {

	@Override
	protected Field<Long> newField(String name) {
		return new LongField(name);
	}

	@Test
	public void test() {
		roundtrip(0L);
		roundtrip(42L);
		roundtrip(Long.MIN_VALUE);
		roundtrip(Long.MAX_VALUE);
	}
}
