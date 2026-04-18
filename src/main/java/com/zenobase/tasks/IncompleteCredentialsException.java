package com.zenobase.tasks;

import java.io.Serial;

public class IncompleteCredentialsException extends CredentialsException {

	@Serial
	private static final long serialVersionUID = 1L;

	private final OAuthCredentials credentials;

	public IncompleteCredentialsException(OAuthCredentials credentials) {
		super("incomplete credentials");
		this.credentials = credentials;
	}

	public OAuthCredentials getCredentials() {
		return credentials;
	}
}
