package com.zenobase.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;

import com.zenobase.json.DomainNode;
import com.zenobase.json.IdentityField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;

public class AuthorizeForm extends DomainNode {

	private static final TokenField RESPONSE_TYPE = new TokenField("response_type");
	private static final IdentityField CLIENT_ID = new IdentityField("client_id");
	private static final TokenField REDIRECT_URI = new TokenField("redirect_uri");
	private static final TokenField SCOPE = new TokenField("scope");

	public AuthorizeForm(ObjectNode node) {
		super(node);
	}

	AuthorizeForm(String responseType, Identity client, String redirectUri, String scope) {
		setValue(RESPONSE_TYPE, responseType);
		setValue(CLIENT_ID, client);
		setValue(REDIRECT_URI, redirectUri);
		setValue(SCOPE, scope);
	}

	public @Nullable String getResponseType() {
		return getValue(RESPONSE_TYPE);
	}

	public @Nullable Identity getClient() {
		return getValue(CLIENT_ID);
	}

	public @Nullable String getRedirectUri() {
		return getValue(REDIRECT_URI);
	}

	public @Nullable String getScope() {
		return getValue(SCOPE);
	}
}
