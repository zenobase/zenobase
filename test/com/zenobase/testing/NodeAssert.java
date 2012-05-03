package com.zenobase.testing;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.fest.assertions.Assertions;
import org.fest.assertions.Delta;
import org.fest.assertions.GenericAssert;

public class NodeAssert extends GenericAssert<NodeAssert, JsonNode> {

	private NodeAssert(JsonNode actual) {
		super(NodeAssert.class, actual);
	}

	public static NodeAssert assertThat(JsonNode actual) {
		return new NodeAssert(actual);
	}

	public NodeAssert isObject() {
		Assertions.assertThat(actual.isObject()).overridingErrorMessage("expected object node but found " + actual).isTrue();
		return this;
	}

	public NodeAssert isArray() {
		Assertions.assertThat(actual.isArray()).overridingErrorMessage("expected array but found " + actual).isTrue();
		return this;
	}

	public NodeAssert isMissingNode() {
		Assertions.assertThat(actual.isMissingNode()).overridingErrorMessage("expected missing node but found " + actual).isTrue();
		return this;
	}

	public NodeAssert isEqualTo(int expected) {
		Assertions.assertThat(actual.isIntegralNumber()).overridingErrorMessage("expected int node but found " + actual).isTrue();
		Assertions.assertThat(actual.getIntValue()).as("int value").isEqualTo(expected);
		return this;
	}

	public NodeAssert isEqualTo(double expected) {
		Assertions.assertThat(actual.isFloatingPointNumber()).overridingErrorMessage("expected floating point node but found " + actual).isTrue();
		Assertions.assertThat(actual.asDouble()).as("double value").isEqualTo(expected, Delta.delta(0.001));
		return this;
	}

	public NodeAssert isEqualTo(String expected) {
		Assertions.assertThat(actual.isTextual()).overridingErrorMessage("expected text node but found " + actual).isTrue();
		Assertions.assertThat(actual.getTextValue()).as("text value").isEqualTo(expected);
		return this;
	}

	public NodeAssert isEqualTo(boolean expected) {
		Assertions.assertThat(actual.isBoolean()).overridingErrorMessage("expected boolean node but found " + actual).isTrue();
		Assertions.assertThat(actual.getBooleanValue()).as("boolean value").isEqualTo(expected);
		return this;
	}

	public NodeAssert path(String fieldName) {
		isObject();
		return new NodeAssert(((ObjectNode) actual).path(fieldName));
	}

	public NodeAssert path(int index) {
		isArray();
		return new NodeAssert(((ArrayNode) actual).path(index));
	}

	public NodeAssert hasSize(int expected) {
		isArray();
		Assertions.assertThat(((ArrayNode) actual).size()).as("array size").isEqualTo(expected);
		return this;
	}
}
