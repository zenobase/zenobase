package com.zenobase.tasks;

import org.scribe.model.OAuthRequest;

public class InvalidStatusException extends OAuthException {

	private static final long serialVersionUID = 1L;

	private final int status;

	public InvalidStatusException(OAuthRequest request, int status) {
		super(request);
		this.status = status;
	}

	@Override
	public String getMessage() {
		return String.format("Request for <%s> returned status <%d>",
			getRequest().getCompleteUrl(), status);
	}
}
