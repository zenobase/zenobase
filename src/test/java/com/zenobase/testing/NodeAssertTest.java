package com.zenobase.testing;

import static com.zenobase.testing.NodeAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.zenobase.json.Nodes;
import org.junit.jupiter.api.Test;

public class NodeAssertTest {

	@Test
	public void testPath() {
		ObjectNode node = Nodes.newObject();
		TextNode nameNode = new TextNode("Foo");
		node.set("name", nameNode);
		assertThat(node).path("name").isEqualTo(nameNode);
	}

	@Test
	public void testMissingNode() {
		assertThat(MissingNode.getInstance()).isMissingNode();
	}

	@Test
	public void testFailMissingNode() {
		assertThatThrownBy(() -> assertThat(Nodes.newObject()).isMissingNode()).isInstanceOf(AssertionError.class);
	}

	@Test
	public void testObject() {
		assertThat(Nodes.newObject()).isObject();
	}

	@Test
	public void testFailObject() {
		assertThatThrownBy(() -> assertThat(Nodes.newArray()).isObject()).isInstanceOf(AssertionError.class);
	}

	@Test
	public void testArray() {
		assertThat(Nodes.newArray()).isArray();
	}

	@Test
	public void testFailArray() {
		assertThatThrownBy(() -> assertThat(Nodes.newObject()).isArray()).isInstanceOf(AssertionError.class);
	}

	@Test
	public void testEqualToText() {
		assertThat(new TextNode("Foo")).isEqualTo("Foo");
	}

	@Test
	public void testFailEqualToText() {
		assertThatThrownBy(() -> assertThat(new TextNode("Foo")).isEqualTo("Bar")).isInstanceOf(AssertionError.class);
	}

	@Test
	public void testEqualToBoolean() {
		assertThat(BooleanNode.TRUE).isEqualTo(true);
	}

	@Test
	public void testFailEqualToBoolean() {
		assertThatThrownBy(() -> assertThat(BooleanNode.TRUE).isEqualTo(false)).isInstanceOf(AssertionError.class);
	}
}
