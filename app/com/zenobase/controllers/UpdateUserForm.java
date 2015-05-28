package com.zenobase.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.BooleanField;
import com.zenobase.json.DomainNode;
import com.zenobase.json.IntegerField;
import com.zenobase.json.TokenField;
import com.zenobase.services.QuotaManager;

public class UpdateUserForm extends DomainNode {

	private static final TokenField EMAIL = new TokenField("email");
	private static final BooleanField VERIFIED = new BooleanField("verified");
	private static final TokenField PASSWORD = new TokenField("password");
	private static final TokenField KEY = new TokenField("key");
	private static final TokenField EXPIRES = new TokenField("expires");
	private static final IntegerField QUOTA = new IntegerField("quota");
	private static final BooleanField SUSPENDED = new BooleanField("suspended");
	private static final BooleanField OPTEDOUT = new BooleanField("optedout");

	public UpdateUserForm(ObjectNode node) {
		super(node);
	}

	UpdateUserForm() {

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

	static UpdateUserForm withOptedOut(boolean optedout) {
		UpdateUserForm form = new UpdateUserForm();
		form.setValue(OPTEDOUT, optedout);
		return form;
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
		Integer quota = getValue(QUOTA);
		return quota != null && quota != QuotaManager.DEFAULT_QUOTA ? quota : null;
	}

	public Boolean isSuspended() {
		return getValue(SUSPENDED);
	}

	public Boolean isOptedOut() {
		return getValue(OPTEDOUT);
	}
}
