package com.zenobase.tasks;

import org.scribe.exceptions.OAuthConnectionException;

public final class ExternalServiceErrors {

	private ExternalServiceErrors() {}

	/** Whether an external service failed in a way that a later attempt will likely get past, e.g. an outage. */
	public static boolean isTransient(Throwable e) {
		return (
			(e instanceof InvalidStatusException status && status.isTransient()) ||
			e instanceof OAuthConnectionException
		);
	}
}
