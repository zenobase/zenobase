package com.zenobase.json;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Collections;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;
import com.google.common.collect.ImmutableList;

import com.zenobase.json.TokenField;

public class TokenFieldTest extends FieldTestSupport {

	private final TokenField field = new TokenField(FIELD_NAME);
	private final ObjectNode node = Nodes.newObject();
	private final ImmutableList<String> values = ImmutableList.of("do", "re", "mi");

	@Test
	public void test() {
		roundtrip(field, "do-re-mi");
	}

	@Test
	public void testGetValueIsNull() {
		assertThat(field.getValue(node)).isNull();
	}

	@Test
	public void testGetValuesIsEmpty() {
		assertThat(field.getValues(node)).isEmpty();
	}

	@Test
	public void testAddValues() {
		field.addValues(node, values);
		assertThat(field.getValues(node)).isEqualTo(values);
		field.addValues(node, values);
		assertThat(field.getValues(node)).hasSize(values.size() * 2);
	}

	@Test
	public void testSetValues() {
		field.setValues(node, values);
		assertThat(field.getValues(node)).isEqualTo(values);
	}

	@Test
	public void testGetValueFromSingletonList() {
		String value = "do";
		field.addValue(node, value);
		assertThat(field.getValue(node)).isEqualTo(value);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetValueFromList() {
		field.addValues(node, values);
		field.getValue(node);
	}

	@Test
	public void testSetValueThenAddValue() {
		field.setValue(node, values.get(0));
		assertThat(field.getValues(node)).isEqualTo(values.subList(0, 1));
		field.addValue(node, values.get(1));
		assertThat(field.getValues(node)).isEqualTo(values.subList(0, 2));
		field.addValue(node, values.get(2));
		assertThat(field.getValues(node)).isEqualTo(values);
	}

	@Test
	public void testSetValueThenAddValues() {
		field.setValue(node, values.get(0));
		field.addValues(node, values.subList(1, values.size()));
		assertThat(field.getValues(node)).isEqualTo(values);
	}

	@Test
	public void testGetValueFromEmptyList() {
		field.addValues(node, Collections.<String>emptyList());
		assertThat(field.getValue(node)).isNull();
	}

	@Test
	public void testSetNullValue() {
		node.put(FIELD_NAME, "do");
		field.setValue(node, null);
		assertThat(field.getValue(node)).as("field value").isNull();
	}
}
