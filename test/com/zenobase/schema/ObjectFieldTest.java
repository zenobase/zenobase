package com.zenobase.schema;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;

import com.zenobase.common.Nodes;

public class ObjectFieldTest extends FieldTestSupport {

	private final ObjectField field = new ObjectField(FIELD_NAME);

	@Test
	public void test() {
		ObjectNode node = Nodes.newObject();
		node.put("name", "Alice");
		node.put("age", 42);
		node.put("vegetarian", true);
		roundtrip(field, node);
	}

	@Test
	public void testEmpty() {
		ObjectNode node = Nodes.newObject();
		roundtrip(field, node);
	}
}
