package com.zenobase.json;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Test;

public class JsonFieldTest extends FieldTestSupport {

	private final JsonField field = new JsonField(FIELD_NAME);

	@Test
	public void testSimpleNodes() {
		roundtrip(field, new IntNode(42));
		roundtrip(field, new TextNode("foo"));
	}

	@Test
	public void testObjectNode() {
		ObjectNode node = Nodes.newObject();
		node.put("foo", 42);
		roundtrip(field, node);
	}
}
