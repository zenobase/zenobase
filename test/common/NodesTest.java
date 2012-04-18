package common;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;

public class NodesTest {

	private static final String FIELD_FRIENDS = "friends";
	private static final String FIELD_NAME = "name";
	private static final String FIELD_PASSWORD = "password";

	@Test
	public void testFilter() {

		ObjectNode node = Nodes.newObject();
		node.put(FIELD_NAME, "me");
		node.put(FIELD_PASSWORD, "xyz");
		ArrayNode friendsNode = node.putArray(FIELD_FRIENDS);
		ObjectNode friendNode = friendsNode.addObject();
		friendNode.put(FIELD_NAME, "you");
		friendNode.put(FIELD_PASSWORD, "abc");
		assertPassword(node, true);

		Nodes.filter(node, Sets.newHashSet(FIELD_PASSWORD));
		assertPassword(node, false);
	}

	private static void assertPassword(ObjectNode node, boolean expected) {
		Assert.assertEquals("me have password in " + node, expected, node.has(FIELD_PASSWORD));
		Assert.assertEquals("you have password in " + node, expected, node.path(FIELD_FRIENDS).get(0).has(FIELD_PASSWORD));
	}
}
