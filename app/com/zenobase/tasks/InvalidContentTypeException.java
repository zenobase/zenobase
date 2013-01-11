package com.zenobase.tasks;

import org.scribe.model.OAuthRequest;

public class InvalidContentTypeException extends OAuthException {

	private static final long serialVersionUID = 1L;

	private final String actual;
	private final String expected;

	public InvalidContentTypeException(OAuthTask task, OAuthRequest request, String actual, String expected) {
		super(task, request);
		this.actual = actual;
		this.expected = expected;
	}

	@Override
	public String getMessage() {
		return String.format("Expected <%s> in task <%s> to return <%s> but got <%s>",
			getRequest().getCompleteUrl(), getTask().getId(), expected, actual);
	}
}
