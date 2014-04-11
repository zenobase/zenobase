package com.zenobase.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.BooleanField;
import com.zenobase.json.DomainNode;
import com.zenobase.json.IntegerField;
import com.zenobase.json.TokenField;

public class UpdateUserForm extends DomainNode {

	private static final TokenField EMAIL = new TokenField("email");
	private static final BooleanField VERIFIED = new BooleanField("verified");
	private static final TokenField PASSWORD = new TokenField("password");
	private static final TokenField KEY = new TokenField("key");
	private static final TokenField EXPIRES = new TokenField("expires");
	private static final IntegerField QUOTA = new IntegerField("quota");
	private static final BooleanField SUSPENDED = new BooleanField("suspended");

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

	UpdateUserForm(boolean suspended) {
		setValue(SUSPENDED, suspended);
	}

	UpdateUserForm(Integer quota) {
		setValue(QUOTA, quota);
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

	public boolean hasQuota() {
		return contains(QUOTA);
	}

	public Integer getQuota() {
		return getValue(QUOTA);
	}

	public Boolean isSuspended() {
		return getValue(SUSPENDED);
	}
}
