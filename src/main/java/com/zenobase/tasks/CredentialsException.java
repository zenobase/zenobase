package com.zenobase.tasks;

import java.io.Serial;

public abstract class CredentialsException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	protected CredentialsException(String message) {
		super(message);
	}
}
