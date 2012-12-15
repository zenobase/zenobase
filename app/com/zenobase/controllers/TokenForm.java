package com.zenobase.controllers;

import java.util.Map;

import org.codehaus.jackson.node.ObjectNode;
import com.google.common.collect.Maps;

import com.zenobase.json.Nodes;

public class TokenForm {

	private String grant_type;
	private String username;
	private String password;

	public TokenForm() {

	}

	public TokenForm(String grant_type, String username, String password) {
		this.grant_type = grant_type;
		this.username = username;
		this.password = password;
	}

	public String getGrant_type() {
		return grant_type;
	}

	public void setGrant_type(String grant_type) {
		this.grant_type = grant_type;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
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
