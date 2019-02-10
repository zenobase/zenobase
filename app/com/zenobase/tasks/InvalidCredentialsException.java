package com.zenobase.tasks;


public class InvalidCredentialsException extends CredentialsException {

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
