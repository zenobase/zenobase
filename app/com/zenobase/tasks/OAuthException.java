package com.zenobase.tasks;

import org.scribe.model.OAuthRequest;

public abstract class OAuthException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final OAuthTask task;
	private final OAuthRequest request;

	public OAuthException(OAuthTask task, OAuthRequest request) {
		this.task = task;
		this.request = request;
	}

	public OAuthTask getTask() {
		return task;
	}

	public OAuthRequest getRequest() {
		return request;
	}
}
