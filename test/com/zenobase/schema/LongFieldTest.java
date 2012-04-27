package com.zenobase.schema;

import org.junit.Test;

public class LongFieldTest extends FieldTestSupport {

	private final LongField field = new LongField(FIELD_NAME);

	@Test
	public void test() {
		roundtrip(field, Long.valueOf(0));
		roundtrip(field, Long.valueOf(42));
	}

	@Test
	public void testMin() {
		roundtrip(field, Long.valueOf(Long.MIN_VALUE));
	}

	@Test
	public void testMax() {
		roundtrip(field, Long.valueOf(Long.MAX_VALUE));
	}
}
