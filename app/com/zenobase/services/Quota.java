package com.zenobase.services;

import org.codehaus.jackson.node.ObjectNode;

import com.zenobase.json.Nodes;

public class Quota {

	private final int limit;
	private final int used;

	public Quota(int limit, int used) {
		this.limit = limit;
		this.used = used;
	}

	public int getLimit() {
		return limit;
	}

	public int getRemaining() {
		return Math.max(limit - used, 0);
	}

	public ObjectNode toJson() {
		ObjectNode node = Nodes.newObject();
		node.put("limit", limit);
		node.put("remaining", Math.max(limit - used, 0));
		return node;
	}
}
