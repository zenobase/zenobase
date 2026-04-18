package com.zenobase.testing;

import com.fasterxml.jackson.databind.JsonNode;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;

import com.zenobase.json.DecimalMeasureField;

public class NodeAssert extends AbstractAssert<NodeAssert, JsonNode> {

	private NodeAssert(JsonNode actual) {
		super(actual, NodeAssert.class);
	}

	public static NodeAssert assertThat(JsonNode actual) {
		return new NodeAssert(actual);
	}

	public NodeAssert isObject() {
		Assertions.assertThat(actual.isObject())
			.overridingErrorMessage("expected object node but found " + actual)
			.isTrue();
		return this;
	}

	public NodeAssert isArray() {
		Assertions.assertThat(actual.isArray()).overridingErrorMessage("expected array but found " + actual).isTrue();
		return this;
	}

	public NodeAssert isMissingNode() {
		Assertions.assertThat(actual.isMissingNode())
			.overridingErrorMessage("expected missing node but found " + actual)
			.isTrue();
		return this;
	}

	public NodeAssert isEqualTo(int expected) {
		Assertions.assertThat(actual.isIntegralNumber())
			.overridingErrorMessage("expected int node but found " + actual)
			.isTrue();
		Assertions.assertThat(actual.intValue()).as("int value").isEqualTo(expected);
		return this;
	}

	public NodeAssert isEqualTo(double expected) {
		Assertions.assertThat(actual.isFloatingPointNumber())
			.overridingErrorMessage("expected floating point node but found " + actual)
			.isTrue();
		Assertions.assertThat(actual.asDouble()).as("double value").isCloseTo(expected, Offset.offset(0.001));
		return this;
	}

	public NodeAssert isEqualTo(String expected) {
		Assertions.assertThat(actual.isTextual())
			.overridingErrorMessage("expected text node but found " + actual)
			.isTrue();
		Assertions.assertThat(actual.textValue()).as("text value").isEqualTo(expected);
		return this;
	}

	public NodeAssert isEqualTo(boolean expected) {
		Assertions.assertThat(actual.isBoolean())
			.overridingErrorMessage("expected boolean node but found " + actual)
			.isTrue();
		Assertions.assertThat(actual.booleanValue()).as("boolean value").isEqualTo(expected);
		return this;
	}

	public void isEqualTo(double expectedValue, String expectedUnit) {
		Assertions.assertThat(actual.isObject())
			.overridingErrorMessage("expected object node but found " + actual)
			.isTrue();
		path(DecimalMeasureField.VALUE.getName()).isEqualTo(expectedValue);
		path(DecimalMeasureField.UNIT.getName()).isEqualTo(expectedUnit);
	}

	public NodeAssert path(String fieldName) {
		isObject();
		return new NodeAssert(actual.path(fieldName));
	}

	public NodeAssert path(int index) {
		isArray();
		return new NodeAssert(actual.path(index));
	}

	public NodeAssert hasSize(int expected) {
		isArray();
		Assertions.assertThat(actual.size()).as("array size").isEqualTo(expected);
		return this;
	}
}
