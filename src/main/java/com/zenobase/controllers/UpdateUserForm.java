package com.zenobase.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.BooleanField;
import com.zenobase.json.DomainNode;
import com.zenobase.json.IntegerField;
import com.zenobase.json.TokenField;
import com.zenobase.services.QuotaManager;
import org.jspecify.annotations.Nullable;

public class UpdateUserForm extends DomainNode {

	private static final TokenField EMAIL = new TokenField("email");
	private static final IntegerField QUOTA = new IntegerField("quota");
	private static final BooleanField SUSPENDED = new BooleanField("suspended");
	private static final BooleanField OPTEDOUT = new BooleanField("optedout");

	public UpdateUserForm(ObjectNode node) {
		super(node);
	}

	UpdateUserForm() {}

	UpdateUserForm(String email) {
		setValue(EMAIL, email);
	}

	UpdateUserForm(boolean suspended) {
		setValue(SUSPENDED, suspended);
	}

	UpdateUserForm(Integer quota) {
		setValue(QUOTA, quota);
	}

	static UpdateUserForm withOptedOut(boolean optedout) {
		var form = new UpdateUserForm();
		form.setValue(OPTEDOUT, optedout);
		return form;
	}

	public @Nullable String getEmail() {
		return getValue(EMAIL);
	}

	public boolean hasQuota() {
		return contains(QUOTA);
	}

	public @Nullable Integer getQuota() {
		Integer quota = getValue(QUOTA);
		return quota != null && quota != QuotaManager.DEFAULT_QUOTA ? quota : null;
	}

	public @Nullable Boolean isSuspended() {
		return getValue(SUSPENDED);
	}

	public @Nullable Boolean isOptedOut() {
		return getValue(OPTEDOUT);
	}
}
