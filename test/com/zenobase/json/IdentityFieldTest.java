package com.zenobase.json;

import org.junit.Test;

import com.zenobase.models.Identity;

public class IdentityFieldTest extends FieldTestSupport<Identity> {

	@Override
	protected Field<Identity> newField(String name) {
		return new IdentityField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(new Identity());
	}
}
