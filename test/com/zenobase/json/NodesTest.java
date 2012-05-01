package com.zenobase.json;

import static com.zenobase.test.NodeAssert.assertThat;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;

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
	public void testRoundTripToBytes() {

		ObjectNode node = Nodes.newObject();
		node.put("name", "Foo");

		byte[] bytes = Nodes.toByteArray(node);
		assertThat(Nodes.read(bytes)).as("deserialized node").isEqualTo(node);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testReadInvalidJson() {

		Nodes.read("{".getBytes());
	}
}
