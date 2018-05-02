package com.zenobase.tasks;

public class MissingCredentialsException extends CredentialsException {

	private static final long serialVersionUID = 1L;

	private final String expectedType;

	public MissingCredentialsException(String expectedType) {
		this.expectedType = expectedType;
	}

	public String getExpectedType() {
		return expectedType;
	}
}
