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

		ObjectNode object = Nodes.newObject();
		object.put(FIELD_NAME, "me");
		object.put(FIELD_PASSWORD, "xyz");
		ArrayNode friendsNode = object.putArray(FIELD_FRIENDS);
		ObjectNode friendNode = friendsNode.addObject();
		friendNode.put(FIELD_NAME, "you");
		friendNode.put(FIELD_PASSWORD, "abc");
		assertPassword(object, true);

		Nodes.filter(object, Sets.newHashSet(FIELD_PASSWORD));
		assertPassword(object, false);
	}

	private static void assertPassword(ObjectNode object, boolean expected) {
		Assert.assertEquals("me have password in " + object, expected, object.has(FIELD_PASSWORD));
		Assert.assertEquals("you have password in " + object, expected, object.path(FIELD_FRIENDS).get(0).has(FIELD_PASSWORD));
	}
}
