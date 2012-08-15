package com.zenobase.json;

import static com.zenobase.testing.NodeAssert.assertThat;

import java.util.List;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;
import com.google.common.collect.Lists;

public class NodesTest {

	@Test
	public void testCopy() {

		ObjectNode node = Nodes.newObject();
		node.putObject("owner").put("name", "Foo");

		ObjectNode copy = Nodes.copy(node);
		assertThat(copy).as("copied node").isEqualTo(node);

		((ObjectNode) copy.get("owner")).put("name", "Bar");
		assertThat(copy).as("copied node after editing a nested field").isNotEqualTo(node);
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
}
