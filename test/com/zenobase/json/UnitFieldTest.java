package com.zenobase.json;

import static org.fest.assertions.Assertions.assertThat;

import javax.measure.quantity.Length;

import org.junit.Test;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.common.Units;

public class UnitFieldTest extends FieldTestSupport {

	private final UnitField<Length> field = new UnitField<Length>(FIELD_NAME);
	private final ObjectNode node = Nodes.newObject();

	@Test
	public void test() {
		roundtrip(field, Units.MI);
	}

	@Test
	public void testGetValueIsNull() {
		assertThat(field.getValue(node)).isNull();
	}
}
