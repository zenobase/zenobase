package com.zenobase.tasks;

import org.scribe.model.OAuthRequest;

public class InvalidStatusException extends OAuthException {

	private static final long serialVersionUID = 1L;

	private final int status;
	private final String body;

	public InvalidStatusException(OAuthRequest request, int status, String body) {
		super(request);
		this.status = status;
		this.body = body;
	}

	public int getStatus() {
		return status;
	}

	@Override
	public String getMessage() {
		return String.format("Request for <%s> returned status <%d>: %s",
			getRequest().getCompleteUrl(), status, body);
	}
}
