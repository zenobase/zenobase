package com.zenobase.json;

import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;

import com.zenobase.testing.NodeAssert;

public class JsonPatchTest {

	private ObjectNode original = Nodes.newObject();
	private ObjectNode from = Nodes.newObject();
	private ObjectNode to = Nodes.newObject();
	private ObjectNode expected = Nodes.newObject();

	@Test
	public void testEmpty() {

		runTest();
	}

	@Test
	public void testReplace() {

		original.put("foo", 41);
		original.put("bar", "xyz");

		from.put("foo", 41);

		to.put("foo", 42);

		expected.put("foo", 42);
		expected.put("bar", "xyz");

		runTest();
	}

	@Test
	public void testAdd() {

		original.put("foo", 41);

		to.put("bar", "xyz");

		expected.put("foo", 41);
		expected.put("bar", "xyz");

		runTest();
	}

	@Test
	public void testRemove() {

		original.put("foo", 41);
		original.put("bar", "xyz");

		from.put("foo", 41);

		to.put("foo", NullNode.getInstance());

		expected.put("bar", "xyz");

		runTest();
	}

	@Test
	public void testMerge() {

		ObjectNode nestedOriginal = Nodes.newObject();
		nestedOriginal.put("foo", 41);
		original.put("nested", nestedOriginal);

		ObjectNode nestedTo = Nodes.newObject();
		nestedTo.put("bar", "xyz");
		to.put("nested", nestedTo);

		ObjectNode nestedExpected= Nodes.newObject();
		nestedExpected.put("foo", 41);
		nestedExpected.put("bar", "xyz");
		expected.put("nested", nestedExpected);

		runTest();
	}

	@Test(expected = IllegalStateException.class)
	public void testValueConflict() {

		original.put("foo", 41);

		from.put("foo", 40);

		to.put("foo", 41);

		expected.put("foo", 41);

		runTest();
	}

	@Test(expected = IllegalStateException.class)
	public void testObjectConflict() {

		ObjectNode nestedOriginal = Nodes.newObject();
		nestedOriginal.put("foo", 41);
		original.put("nested", nestedOriginal);

		ObjectNode nestedFrom = Nodes.newObject();
		nestedFrom.put("foo", 40);
		from.put("nested", nestedFrom);

		ObjectNode nestedTo = Nodes.newObject();
		nestedTo.put("foo", 41);
		to.put("nested", nestedTo);

		ObjectNode nestedExpected= Nodes.newObject();
		nestedExpected.put("foo", 41);
		expected.put("nested", nestedExpected);

		runTest();
	}

	private void runTest() {
		NodeAssert.assertThat(new JsonPatch(from, to).apply(original)).isEqualTo(expected);
	}
}
