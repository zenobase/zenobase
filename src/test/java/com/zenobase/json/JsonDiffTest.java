package com.zenobase.json;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.testing.NodeAssert;
import org.junit.jupiter.api.Test;

public class JsonDiffTest {

	@Test
	public void test() {
		ObjectNode original = Nodes.newObject();
		original.put("a", 1);
		original.put("b", false);
		original.put("c", "xyz");

		ObjectNode modified = Nodes.newObject();
		modified.put("a", 1);
		modified.put("b", true);
		modified.put("d", "xyz");

		test(original, modified);
	}

	@Test
	public void testNestedModification() {
		ObjectNode original = Nodes.newObject();
		ObjectNode orifinalNested = Nodes.newObject();
		orifinalNested.put("value", 1);
		original.set("a", orifinalNested);

		ObjectNode modified = Nodes.newObject();
		ObjectNode modifiedNested = Nodes.newObject();
		modifiedNested.put("value", 2);
		modified.set("a", modifiedNested);

		test(original, modified);
	}

	@Test
	public void testNestedUnchanged() {
		ObjectNode original = Nodes.newObject();
		ObjectNode orifinalNested = Nodes.newObject();
		orifinalNested.put("value", 1);
		original.set("a", orifinalNested);

		ObjectNode modified = Nodes.newObject();
		ObjectNode modifiedNested = Nodes.newObject();
		modifiedNested.put("value", 1);
		modified.set("a", modifiedNested);

		test(original, modified);
	}

	@Test
	public void testValueToObject() {
		ObjectNode original = Nodes.newObject();
		original.put("a", 1);

		ObjectNode modified = Nodes.newObject();
		ObjectNode nested = Nodes.newObject();
		nested.put("value", 1);
		modified.set("a", nested);

		test(original, modified);
	}

	@Test
	public void testObjectToValue() {
		ObjectNode original = Nodes.newObject();
		ObjectNode nested = Nodes.newObject();
		nested.put("value", 1);
		original.set("a", nested);

		ObjectNode modified = Nodes.newObject();
		modified.put("a", 1);

		test(original, modified);
	}

	private static void test(ObjectNode original, ObjectNode modified) {
		JsonPatch patch = new JsonDiff().diff(original, modified);
		NodeAssert.assertThat(patch.apply(original)).isEqualTo(modified);
	}
}
