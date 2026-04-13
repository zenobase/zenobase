package com.zenobase.controllers;

import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;

import com.zenobase.json.Nodes;

public class TokenForm {

	private @Nullable String grant_type;

	public TokenForm() {}

	public TokenForm(@Nullable String grant_type) {
		this.grant_type = grant_type;
	}

	public @Nullable String getGrant_type() {
		return grant_type;
	}

	public void setGrant_type(@Nullable String grant_type) {
		this.grant_type = grant_type;
	}

	public Map<String, String> toMap() {
		return Map.of("grant_type", grant_type != null ? grant_type : "");
	}

	public ObjectNode toJson() {
		ObjectNode node = Nodes.newObject();
		node.put("grant_type", grant_type);
		return node;
	}
}
