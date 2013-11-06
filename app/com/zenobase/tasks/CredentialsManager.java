package com.zenobase.tasks;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.commands.Command;
import com.zenobase.models.Identity;

public abstract class CredentialsManager {

	private final String type;

	protected CredentialsManager(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public abstract Credentials newCredentials(Identity principal);

	public Command authorize(Credentials credentials, ObjectNode config) {
		throw new UnsupportedOperationException();
	}
}
