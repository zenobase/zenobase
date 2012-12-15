package com.zenobase.controllers;

import org.codehaus.jackson.node.ObjectNode;

import com.zenobase.json.DomainNode;
import com.zenobase.json.TokenField;

public class TokenForm extends DomainNode {

	private static final TokenField GRANT_TYPE = new TokenField("grant_type");
	private static final TokenField USERNAME = new TokenField("username");
	private static final TokenField PASSWORD = new TokenField("password");

	public TokenForm(ObjectNode node) {
		super(node);
	}

	TokenForm(String grantType, String username, String password) {
		setValue(GRANT_TYPE, grantType);
		setValue(USERNAME, username);
		setValue(PASSWORD, password);
	}

	public String getGrantType() {
		return getValue(GRANT_TYPE);
	}

	public String getUsername() {
		return getValue(USERNAME);
	}

	public String getPassword() {
		return getValue(PASSWORD);
	}
}
