package com.zenobase.models;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.primitives.Ints;

import com.zenobase.common.PartialList;
import com.zenobase.json.LazyList;
import com.zenobase.json.Nodes;

public class UserList extends LazyList<User> {

	public UserList(PartialList<ObjectNode> nodes) {
		super(nodes);
	}

	@Override
	protected User toObject(ObjectNode node) {
		return new User(node);
	}

	public static ObjectNode toJson(PartialList<User> users) {
		ObjectNode resultNode = Nodes.newObject();
		TOTAL.setValue(resultNode, Ints.checkedCast(users.getTotal()));
		ArrayNode usersNode = resultNode.putArray("users");
		for (User user : users) {
			usersNode.add(new UserProfile(user).toJson());
		}
		return resultNode;
	}
}
