package com.zenobase.tasks;

import org.codehaus.jackson.node.ArrayNode;

class TwitterTimelineNode {

	private final ArrayNode node;

	public TwitterTimelineNode(ArrayNode node) {
		this.node = node;
	}

	public int size() {
		return node.size();
	}
}
