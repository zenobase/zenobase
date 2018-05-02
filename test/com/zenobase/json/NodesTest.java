package com.zenobase.json;

import static com.zenobase.testing.NodeAssert.assertThat;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.fest.assertions.Assertions;
import org.junit.Test;

public class NodesTest {

	@Test
	public void testNewObject() {

		ObjectNode node = Nodes.newObject("name", "Foo");
		assertThat(node).path("name").isEqualTo("Foo");
	}

	@Test
	public void testRoundTripObjectToBytes() {

		ObjectNode node = Nodes.newObject();
		node.put("name", "Foo");

		byte[] bytes = Nodes.toByteArray(node);
		assertThat(Nodes.readObject(bytes)).as("deserialized object node").isEqualTo(node);
	}

	@Test
	public void testRoundTripArrayToBytes() {

		List<String> values = Lists.newArrayList("foo", "bar");
		ArrayNode node = Nodes.newArray(values);

		byte[] bytes = Nodes.toByteArray(node);
		assertThat(Nodes.readArray(bytes)).as("deserialized array node").isEqualTo(node);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testReadInvalidJson() {

		Nodes.readObject("{".getBytes());
	}

	@Test
	public void testRoundTripBigDecimal() {

		ObjectNode node = Nodes.newObject();
		node.put("value", new BigDecimal("1.00"));

		byte[] bytes = Nodes.toByteArray(node);
		assertThat(Nodes.readObject(bytes)).as("deserialized object node").isEqualTo(node);
	}

	@Test
	public void testSize() {

		Assertions.assertThat(Nodes.size(null)).as("null").isEqualTo(0);
		Assertions.assertThat(Nodes.size(Nodes.newObject())).as("empty object node").isEqualTo(0);
		Assertions.assertThat(Nodes.size(Nodes.newArray())).as("empty array node").isEqualTo(0);
		Assertions.assertThat(Nodes.size(new IntNode(42))).as("int node").isEqualTo(0);
		Assertions.assertThat(Nodes.size(Nodes.newObject("name", "Foo"))).as("object node with one field").isEqualTo(1);
	}

    @Test
    public void testParseEmptyString() {

        assertThat(Nodes.read("")).isEqualTo(MissingNode.getInstance());
        assertThat(Nodes.read((String) null)).isEqualTo(MissingNode.getInstance());
    }
}
