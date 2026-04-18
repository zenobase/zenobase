package com.zenobase.tasks;

import java.io.Serial;

public class MissingCredentialsException extends CredentialsException {

	@Serial
	private static final long serialVersionUID = 1L;

	private final String expectedType;

	public MissingCredentialsException(String expectedType) {
		super("missing credentials of type " + expectedType);
		this.expectedType = expectedType;
	}

	public String getExpectedType() {
		return expectedType;
	}
}
