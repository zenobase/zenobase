package com.zenobase.tasks;

import java.io.Serial;

public class InvalidTokenException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	private final OAuthCredentials credentials;

	public InvalidTokenException(OAuthCredentials credentials) {
		this.credentials = credentials;
	}

	public OAuthCredentials getCredentials() {
		return credentials;
	}
}
