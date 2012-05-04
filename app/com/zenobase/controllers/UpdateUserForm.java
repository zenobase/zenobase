package com.zenobase.controllers;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;

import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.DomainNode;

public class UpdateUserForm extends DomainNode {

	private static final TokenField EMAIL = new TokenField("email");
	private static final BooleanField VERIFIED = new BooleanField("verified");
	private static final TokenField PASSWORD = new TokenField("password");
	private static final TokenField KEY = new TokenField("key");
	private static final TokenField EXPIRES = new TokenField("expires");

	public UpdateUserForm(ObjectNode node) {
		super(node);
	}

	UpdateUserForm(String email) {
		setValue(EMAIL, email);
	}

	UpdateUserForm(Boolean verified, String key) {
		setValue(VERIFIED, verified);
		setValue(KEY, key);
	}

	UpdateUserForm(String password, String key, String expires) {
		setValue(PASSWORD, password);
		setValue(KEY, key);
		setValue(EXPIRES, expires);
	}

	public String getEmail() {
		return getValue(EMAIL);
	}

	public boolean isVerified() {
		return getValue(VERIFIED) == Boolean.TRUE;
	}

	public String getPassword() {
		return getValue(PASSWORD);
	}

	public String getKey() {
		return getValue(KEY);
	}

	public String getExpires() {
		return getValue(EXPIRES);
	}

	public DateTime getExpiresDate() {
		String value = getValue(EXPIRES);
		return value != null ? new DateTime(Long.parseLong(value, 36)) : null;
	}
}
