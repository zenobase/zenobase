package com.zenobase.json;

import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.Alias;

public class AliasFieldTest extends FieldTestSupport {

	@Test
	public void test() {
		roundtrip(new AliasField(FIELD_NAME), new Alias(Generator.id(), "tag:foo"));
	}
}
