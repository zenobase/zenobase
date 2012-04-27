package com.zenobase.json;

import com.zenobase.json.TextField;

import org.junit.Test;

public class TextFieldTest extends FieldTestSupport {

	@Test
	public void test() {
		roundtrip(new TextField(FIELD_NAME), "do-re-mi");
	}
}
