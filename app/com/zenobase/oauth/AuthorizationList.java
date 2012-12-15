package com.zenobase.oauth;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import com.google.common.primitives.Ints;

import com.zenobase.common.PartialList;
import com.zenobase.json.Nodes;

public class AuthorizationList extends PartialList<Authorization> {

	public AuthorizationList(Iterable<Authorization> elements, long size) {
		super(elements, size);
	}

	public ObjectNode toJson() {
    	ObjectNode resultNode = Nodes.newObject();
    	TOTAL.setValue(resultNode, Ints.checkedCast(size()));
    	ArrayNode usersNode = resultNode.putArray("authorizations");
    	for (Authorization auth : getElements()) {
    		usersNode.add(auth.toJson());
    	}
		return resultNode;
	}
}
