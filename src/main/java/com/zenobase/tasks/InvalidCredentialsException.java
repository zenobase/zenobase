package com.zenobase.tasks;

import java.io.Serial;

public class InvalidCredentialsException extends CredentialsException {

	@Serial
	private static final long serialVersionUID = 1L;

	private final OAuthCredentials credentials;

	public InvalidCredentialsException(OAuthCredentials credentials) {
		super("invalid credentials");
		this.credentials = credentials;
	}

	public OAuthCredentials getCredentials() {
		return credentials;
	}
}
