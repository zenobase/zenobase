package com.zenobase.json;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.common.DefaultPartialList;

public class NodeList extends DefaultPartialList<ObjectNode> {

	public NodeList(Iterable<ObjectNode> nodes, long total) {
		super(nodes, total);
	}
}
