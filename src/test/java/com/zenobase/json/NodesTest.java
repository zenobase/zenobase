package com.zenobase.json;

import static com.zenobase.testing.NodeAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

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

	@Test
	public void testReadInvalidJson() {
		assertThatThrownBy(() -> Nodes.readObject("{".getBytes())).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testRoundTripBigDecimal() {
		ObjectNode node = Nodes.newObject();
		node.put("value", new BigDecimal("1.00"));

		byte[] bytes = Nodes.toByteArray(node);
		assertThat(Nodes.readObject(bytes)).as("deserialized object node").isEqualTo(node);
	}

	@Test
	public void testParseEmptyString() {
		assertThat(Nodes.read("")).isEqualTo(MissingNode.getInstance());
		assertThat(Nodes.read((String) null)).isEqualTo(MissingNode.getInstance());
	}
}
