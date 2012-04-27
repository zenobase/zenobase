package com.zenobase.json;

import com.zenobase.json.BooleanField;

import org.junit.Test;

public class BooleanFieldTest extends FieldTestSupport {

	private final BooleanField field = new BooleanField(FIELD_NAME);

	@Test
	public void test() {
		roundtrip(field, Boolean.TRUE);
		roundtrip(field, Boolean.FALSE);
	}
}
