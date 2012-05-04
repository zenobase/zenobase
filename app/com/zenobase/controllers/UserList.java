package com.zenobase.controllers;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import com.google.common.primitives.Ints;

import com.zenobase.common.PartialList;
import com.zenobase.json.Nodes;
import com.zenobase.models.User;
import com.zenobase.models.UserProfile;

public class UserList extends PartialList<User> {

	public UserList(Iterable<User> elements, long size) {
		super(elements, size);
	}

	public ObjectNode toJson() {
    	ObjectNode resultNode = Nodes.newObject();
    	TOTAL.setValue(resultNode, Ints.checkedCast(size()));
    	ArrayNode usersNode = resultNode.putArray("users");
    	for (User user : getElements()) {
    		usersNode.add(new UserProfile(user).toJson());
    	}
		return resultNode;
	}
}
