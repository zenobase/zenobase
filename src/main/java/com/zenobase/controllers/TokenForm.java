package com.zenobase.controllers;

import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import org.jspecify.annotations.Nullable;

import com.zenobase.json.Nodes;

public class TokenForm {

	private @Nullable String grant_type;
	private @Nullable String username;
	private @Nullable String password;

	public TokenForm() {}

	public TokenForm(@Nullable String grant_type, @Nullable String username, @Nullable String password) {
		this.grant_type = grant_type;
		this.username = username;
		this.password = password;
	}

	public @Nullable String getGrant_type() {
		return grant_type;
	}

	public void setGrant_type(@Nullable String grant_type) {
		this.grant_type = grant_type;
	}

	public @Nullable String getUsername() {
		return username;
	}

	public void setUsername(@Nullable String username) {
		this.username = username;
	}

	public @Nullable String getPassword() {
		return password;
	}

	public void setPassword(@Nullable String password) {
		this.password = password;
	}

	public Map<String, String> toMap() {
		Map<String, String> map = Maps.newHashMap();
		map.put("grant_type", grant_type);
		if (username != null) {
			map.put("username", username);
		}
		if (password != null) {
			map.put("password", password);
		}
		return map;
	}

	public ObjectNode toJson() {
		ObjectNode node = Nodes.newObject();
		node.put("grant_type", grant_type);
		if (username != null) {
			node.put("username", username);
		}
		if (password != null) {
			node.put("password", password);
		}
		return node;
	}
}
