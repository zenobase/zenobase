package com.zenobase.json;

import com.zenobase.common.Generator;
import com.zenobase.models.Alias;
import org.junit.jupiter.api.Test;

public class AliasFieldTest extends FieldTestSupport<Alias> {

	@Override
	protected Field<Alias> newField(String name) {
		return new AliasField(name);
	}

	@Test
	public void test() {
		roundtrip(new Alias(Generator.id(), "tag:foo"));
	}
}
