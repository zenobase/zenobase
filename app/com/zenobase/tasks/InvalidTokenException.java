package com.zenobase.tasks;

import org.scribe.model.OAuthRequest;

public class InvalidTokenException extends OAuthException {

	private static final long serialVersionUID = 1L;

	public InvalidTokenException(OAuthTask task, OAuthRequest request) {
		super(task, request);
	}
}
