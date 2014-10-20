package com.zenobase.json;

import org.junit.Test;

public class TextFieldTest extends FieldTestSupport<String> {

	@Override
	protected Field<String> newField(String name) {
		return new TextField(name);
	}
	@Test
	public void test() {
		roundtrip("do-re-mi");
	}
}
