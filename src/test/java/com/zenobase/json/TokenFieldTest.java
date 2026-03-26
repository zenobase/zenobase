package com.zenobase.json;

import org.junit.jupiter.api.Test;

public class TokenFieldTest extends FieldTestSupport<String> {

	@Override
	protected Field<String> newField(String name) {
		return new TokenField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip("do-re mi");
	}
}
