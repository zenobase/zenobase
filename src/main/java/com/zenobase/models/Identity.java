package com.zenobase.models;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;

public class Identity {

	public static final Identity PUBLIC = new Identity("*");

	private final String id;

	public Identity() {
		this(Generator.id());
	}

	public Identity(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof Identity && equals((Identity) that);
	}

	private boolean equals(Identity that) {
		return id.equals(that.getId());
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
