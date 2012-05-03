package com.zenobase.testing;

import static com.zenobase.testing.NodeAssert.assertThat;

import org.codehaus.jackson.node.BooleanNode;
import org.codehaus.jackson.node.MissingNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;
import org.junit.Test;

import com.zenobase.json.Nodes;

public class NodeAssertTest {

	@Test
	public void testPath() {
		ObjectNode node = Nodes.newObject();
		TextNode nameNode = new TextNode("Foo");
		node.put("name", nameNode);
		assertThat(node).path("name").isEqualTo(nameNode);
	}

	@Test
	public void testMissingNode() {
		assertThat(MissingNode.getInstance()).isMissingNode();
	}

	@Test(expected = AssertionError.class)
	public void testFailMissingNode() {
		assertThat(Nodes.newObject()).isMissingNode();
	}

	@Test
	public void testObject() {
		assertThat(Nodes.newObject()).isObject();
	}

	@Test(expected = AssertionError.class)
	public void testFailObject() {
		assertThat(Nodes.newArray()).isObject();
	}

	@Test
	public void testArray() {
		assertThat(Nodes.newArray()).isArray();
	}

	@Test(expected = AssertionError.class)
	public void testFailArray() {
		assertThat(Nodes.newObject()).isArray();
	}

	@Test
	public void testEqualToText() {
		assertThat(new TextNode("Foo")).isEqualTo("Foo");
	}

	@Test(expected = AssertionError.class)
	public void testFailEqualToText() {
		assertThat(new TextNode("Foo")).isEqualTo("Bar");
	}

	@Test
	public void testEqualToBoolean() {
		assertThat(BooleanNode.TRUE).isEqualTo(true);
	}

	@Test(expected = AssertionError.class)
	public void testFailEqualToBoolean() {
		assertThat(BooleanNode.TRUE).isEqualTo(false);
	}
}
