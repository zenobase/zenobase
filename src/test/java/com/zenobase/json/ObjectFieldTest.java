package com.zenobase.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

public class ObjectFieldTest extends FieldTestSupport<ObjectNode> {

	@Override
	protected Field<ObjectNode> newField(String name) {
		return new ObjectField(name);
	}

	@Test
	public void test() {
		roundtrip(null);
		roundtrip(Nodes.newObject());
		roundtrip(Nodes.newObject("name", "Alice").put("age", 42).put("vegetarian", true));
	}
}
