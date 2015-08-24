package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.Test;

public class JsonFieldTest extends FieldTestSupport<JsonNode> {

	@Override
	protected Field<JsonNode> newField(String name) {
		return new JsonField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(new IntNode(42));
		roundtrip(new TextNode("foo"));
		roundtrip(Nodes.newObject("foo", "bar"));
	}
}
