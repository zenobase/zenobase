package com.zenobase.tasks;

import org.scribe.model.OAuthRequest;

public abstract class OAuthException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final OAuthRequest request;

	public OAuthException(OAuthRequest request) {
		this.request = request;
	}

	public OAuthRequest getRequest() {
		return request;
	}
}
