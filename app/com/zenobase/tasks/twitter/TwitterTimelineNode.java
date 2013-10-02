package com.zenobase.tasks.twitter;

import com.fasterxml.jackson.databind.node.ArrayNode;

class TwitterTimelineNode {

	private final ArrayNode node;

	public TwitterTimelineNode(ArrayNode node) {
		this.node = node;
	}

	public int size() {
		return node.size();
	}
}
