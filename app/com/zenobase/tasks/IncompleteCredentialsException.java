package com.zenobase.tasks;


public class IncompleteCredentialsException extends CredentialsException {

	private static final long serialVersionUID = 1L;

	private final OAuthCredentials credentials;

	public IncompleteCredentialsException(OAuthCredentials credentials) {
		this.credentials = credentials;
	}

	public OAuthCredentials getCredentials() {
		return credentials;
	}
}
