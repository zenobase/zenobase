package com.zenobase.tasks;

import com.zenobase.common.Urls;
import java.io.Serial;

public class InvalidStatusException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	private final int status;

	public InvalidStatusException(String url, int status, String body) {
		super(String.format("Request for <%s> returned status <%d>: %s", Urls.redact(url), status, body));
		this.status = status;
	}

	public int getStatus() {
		return status;
	}

	/** An outage or rate limit on the provider's side that will likely clear up on a later refresh. */
	public boolean isTransient() {
		return status >= 500 || status == 429;
	}
}
