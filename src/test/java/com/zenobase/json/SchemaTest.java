package com.zenobase.json;

import static com.zenobase.testing.NodeAssert.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

public class SchemaTest {

	private final Schema schema = new SchemaBuilder("test")
		.add(new TokenField("who"))
		.add(new TokenField("what"))
		.build();

	@Test
	public void sanitizeRemovesUnknownFields() {
		ObjectNode node = Nodes.newObject();
		node.put("who", "alice");
		node.put("what", "widget");
		node.put("legacy", "x");

		ObjectNode result = schema.sanitize(node);

		assertThat(result).path("who").isEqualTo("alice");
		assertThat(result).path("what").isEqualTo("widget");
		assertThat(result).path("legacy").isMissingNode();
	}

	@Test
	public void sanitizeEmptyNode() {
		ObjectNode result = schema.sanitize(Nodes.newObject());

		assertThat(result).isObject();
		assertThat(result).path("who").isMissingNode();
	}

	@Test
	public void sanitizeAllKnown() {
		ObjectNode node = Nodes.newObject();
		node.put("who", "alice");

		ObjectNode result = schema.sanitize(node);

		assertThat(result).path("who").isEqualTo("alice");
	}

	@Test
	public void sanitizeDoesNotMutateInput() {
		ObjectNode node = Nodes.newObject();
		node.put("who", "alice");
		node.put("legacy", "x");

		schema.sanitize(node);

		assertThat(node).path("who").isEqualTo("alice");
		assertThat(node).path("legacy").isEqualTo("x");
	}
}
