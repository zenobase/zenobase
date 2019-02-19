package com.zenobase.commands;

public class NonExistentUserException extends RuntimeException {

	public NonExistentUserException(String message) {
		super(message);
	}
}
