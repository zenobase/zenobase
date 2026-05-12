package com.zenobase.models;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.common.PartialList;
import com.zenobase.json.LazyList;

public class ExternalClientList extends LazyList<ExternalClient> {

	public ExternalClientList(PartialList<ObjectNode> nodes) {
		super(nodes);
	}

	@Override
	protected ExternalClient toObject(ObjectNode node) {
		return new ExternalClient(node);
	}
}
