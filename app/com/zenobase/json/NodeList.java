package com.zenobase.json;

import org.codehaus.jackson.node.ObjectNode;

import com.zenobase.common.DefaultPartialList;

public class NodeList extends DefaultPartialList<ObjectNode> {

	public NodeList(Iterable<ObjectNode> nodes, long total) {
		super(nodes, total);
	}
}
