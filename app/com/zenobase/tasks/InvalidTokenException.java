package com.zenobase.tasks;

import org.scribe.model.OAuthRequest;

public class InvalidTokenException extends OAuthException {

	private static final long serialVersionUID = 1L;

	private final OAuthCredentials credentials;

	public InvalidTokenException(OAuthRequest request, OAuthCredentials credentials) {
		super(request);
		this.credentials = credentials;
	}

	public OAuthCredentials getCredentials() {
		return credentials;
	}
}
