package com.zenobase.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.DomainNode;
import com.zenobase.tasks.Credentials;

public class CreateCredentialsForm extends DomainNode {

	public CreateCredentialsForm(ObjectNode node) {
		super(node);
	}

	public CreateCredentialsForm(String type) {
		super();
		setValue(Credentials.TYPE, type);
	}

	public String getType() {
		return getValue(Credentials.TYPE);
	}

	public boolean valid() {
		return getType() != null;
	}
}
