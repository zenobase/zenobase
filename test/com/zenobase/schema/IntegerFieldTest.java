package com.zenobase.schema;

import org.junit.Test;

public class IntegerFieldTest extends FieldTestSupport {

	private final IntegerField field = new IntegerField(FIELD_NAME);

	@Test
	public void test() {
		roundtrip(field, Integer.valueOf(0));
		roundtrip(field, Integer.valueOf(42));
	}

	@Test
	public void testMin() {
		roundtrip(field, Integer.valueOf(Integer.MIN_VALUE));
	}

	@Test
	public void testMax() {
		roundtrip(field, Integer.valueOf(Integer.MAX_VALUE));
	}
}
