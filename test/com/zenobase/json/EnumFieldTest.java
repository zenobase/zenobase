package com.zenobase.json;

import com.fasterxml.jackson.databind.node.NullNode;
import org.fest.assertions.Assertions;
import org.junit.Test;

import com.zenobase.testing.NodeAssert;

public class EnumFieldTest extends FieldTestSupport {

	private enum Option {
		A, B, C
	}

	private EnumField<Option> field = EnumField.newInstance(FIELD_NAME, Option.class);

	@Test
	public void test() {
		roundtrip(field, Option.A);
	}

	@Test
	public void testNull() {
		Assertions.assertThat(field.getValue(NullNode.getInstance())).isNull();
		NodeAssert.assertThat(field.toJson(null)).isEqualTo(NullNode.getInstance());
	}
}
