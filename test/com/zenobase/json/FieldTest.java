package com.zenobase.json;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Collections;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class FieldTest {

	private static final String FIELD_NAME = "field";
	private static final TokenField FIELD = new TokenField(FIELD_NAME);
	private static final ImmutableList<String> VALUES = ImmutableList.of("do", "re", "mi");

	private final ObjectNode node = Nodes.newObject();

	@Test
	public void testGetValueIsNull() {
		assertThat(FIELD.getValue(node)).isNull();
	}

	@Test
	public void testGetValuesIsEmpty() {
		assertThat(FIELD.getValues(node)).isEmpty();
	}

	@Test
	public void testAddValues() {
		FIELD.addValues(node, VALUES);
		assertThat(FIELD.getValues(node)).isEqualTo(VALUES);
		FIELD.addValues(node, VALUES);
		assertThat(FIELD.getValues(node)).hasSize(VALUES.size() * 2);
	}

	@Test
	public void testSetValues() {
		FIELD.setValues(node, VALUES);
		assertThat(FIELD.getValues(node)).isEqualTo(VALUES);
	}

	@Test
	public void testGetValueFromSingletonList() {
		String value = "do";
		FIELD.addValue(node, value);
		assertThat(FIELD.getValue(node)).isEqualTo(value);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetValueFromList() {
		FIELD.addValues(node, VALUES);
		FIELD.getValue(node);
	}

	@Test
	public void testSetValueThenAddValue() {
		FIELD.setValue(node, VALUES.get(0));
		assertThat(FIELD.getValues(node)).isEqualTo(VALUES.subList(0, 1));
		FIELD.addValue(node, VALUES.get(1));
		assertThat(FIELD.getValues(node)).isEqualTo(VALUES.subList(0, 2));
		FIELD.addValue(node, VALUES.get(2));
		assertThat(FIELD.getValues(node)).isEqualTo(VALUES);
	}

	@Test
	public void testSetValueThenAddValues() {
		FIELD.setValue(node, VALUES.get(0));
		FIELD.addValues(node, VALUES.subList(1, VALUES.size()));
		assertThat(FIELD.getValues(node)).isEqualTo(VALUES);
	}

	@Test
	public void testGetValueFromEmptyList() {
		FIELD.addValues(node, Collections.<String>emptyList());
		assertThat(FIELD.getValue(node)).isNull();
	}

	@Test
	public void testSetNullValue() {
		node.put(FIELD_NAME, "do");
		FIELD.setValue(node, null);
		assertThat(FIELD.getValue(node)).as("field value").isNull();
	}
}
