package com.zenobase.tasks;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.primitives.Ints;
import com.zenobase.common.PartialList;
import com.zenobase.json.LazyList;
import com.zenobase.json.Nodes;

public class CredentialsList extends LazyList<Credentials> {

	public CredentialsList(PartialList<ObjectNode> nodes) {
		super(nodes);
	}

	@Override
	protected Credentials toObject(ObjectNode node) {
		return new Credentials(node);
	}

	public static ObjectNode toJson(PartialList<Credentials> credentials) {
		ObjectNode node = Nodes.newObject();
		TOTAL.setValue(node, Ints.checkedCast(credentials.getTotal()));
		ArrayNode itemsNode = node.putArray("items");
		for (Credentials integration : credentials) {
			itemsNode.add(integration.sanitized().toJson());
		}
		return node;
	}
}
