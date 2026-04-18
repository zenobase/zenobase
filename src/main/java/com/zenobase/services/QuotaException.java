package com.zenobase.services;

import java.io.Serial;

public class QuotaException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	private final int remaining;
	private final int required;

	public QuotaException(int remaining, int required) {
		super("Quota remaining: " + remaining + ", required: " + required);
		this.remaining = remaining;
		this.required = required;
	}

	public int getRemaining() {
		return remaining;
	}

	public int getRequired() {
		return required;
	}
}
