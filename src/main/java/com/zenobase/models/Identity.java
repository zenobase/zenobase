package com.zenobase.models;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;

public record Identity(String id) {

	public static final Identity PUBLIC = new Identity("*");

	public Identity() {
		this(Generator.id());
	}

	@Override
	public String toString() {
		return id;
	}

	public ObjectNode toJson() {
		ObjectNode node = Nodes.newObject();
		node.put(User.ID.getName(), id);
		return node;
	}
}
