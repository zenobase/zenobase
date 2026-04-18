package com.zenobase.commands;

import java.io.Serial;

public class NonExistentUserException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	public NonExistentUserException(String message) {
		super(message);
	}
}
