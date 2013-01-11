package com.zenobase.tasks;

import org.scribe.model.OAuthRequest;

public class InvalidStatusException extends OAuthException {

	private static final long serialVersionUID = 1L;

	private final int status;

	public InvalidStatusException(OAuthTask task, OAuthRequest request, int status) {
		super(task, request);
		this.status = status;
	}

	@Override
	public String getMessage() {
		return String.format("Request for <%s> in task <%s> returned status <%d>",
			getRequest().getCompleteUrl(), getTask().getId(), status);
	}
}
