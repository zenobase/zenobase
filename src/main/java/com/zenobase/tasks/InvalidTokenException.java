package com.zenobase.tasks;

public class InvalidTokenException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final OAuthCredentials credentials;

	public InvalidTokenException(OAuthCredentials credentials) {
		this.credentials = credentials;
	}

	public OAuthCredentials getCredentials() {
		return credentials;
	}
}
