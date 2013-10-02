package com.zenobase.oauth;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.primitives.Ints;

import com.zenobase.common.PartialList;
import com.zenobase.json.LazyList;
import com.zenobase.json.Nodes;

public class AuthorizationList extends LazyList<Authorization> {

	public AuthorizationList(PartialList<ObjectNode> nodes) {
		super(nodes);
	}

	@Override
	protected Authorization toObject(ObjectNode node) {
		return new Authorization(node);
	}

	public static ObjectNode toJson(PartialList<Authorization> authorizations) {
    	ObjectNode resultNode = Nodes.newObject();
    	TOTAL.setValue(resultNode, Ints.checkedCast(authorizations.getTotal()));
    	ArrayNode usersNode = resultNode.putArray("authorizations");
    	for (Authorization authorization : authorizations) {
    		usersNode.add(authorization.toJson());
    	}
		return resultNode;
	}
}
