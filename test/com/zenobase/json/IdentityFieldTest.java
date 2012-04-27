package com.zenobase.json;

import org.junit.Test;

import com.zenobase.json.IdentityField;
import com.zenobase.models.Identity;

public class IdentityFieldTest extends FieldTestSupport {

	@Test
	public void test() {
		roundtrip(new IdentityField(FIELD_NAME), new Identity());
	}
}
