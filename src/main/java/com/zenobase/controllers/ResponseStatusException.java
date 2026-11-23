package com.zenobase.controllers;

import io.helidon.http.Status;
import io.helidon.webserver.http.ServerResponse;
import org.jspecify.annotations.Nullable;

/**
 * Thrown by the {@code require*} guards in {@link ControllerSupport} to end a request early. Rendered by the routing
 * error handler via {@link #send}, which writes the same response as the equivalent {@code sendX} helper: an empty body
 * when there is no message, otherwise the JSON error body of {@link ControllerSupport#sendError}.
 */
public final class ResponseStatusException extends RuntimeException {

	private final Status status;

	private ResponseStatusException(Status status, @Nullable String message) {
		super(message, null, false, false);
		this.status = status;
	}

	public static ResponseStatusException badRequest(String message) {
		return new ResponseStatusException(Status.BAD_REQUEST_400, message);
	}

	public static ResponseStatusException unauthorized() {
		return new ResponseStatusException(Status.UNAUTHORIZED_401, null);
	}

	public static ResponseStatusException forbidden() {
		return new ResponseStatusException(Status.FORBIDDEN_403, null);
	}

	public static ResponseStatusException forbidden(String message) {
		return new ResponseStatusException(Status.FORBIDDEN_403, message);
	}

	public static ResponseStatusException notFound() {
		return new ResponseStatusException(Status.NOT_FOUND_404, null);
	}

	public static ResponseStatusException notFound(String message) {
		return new ResponseStatusException(Status.NOT_FOUND_404, message);
	}

	public static ResponseStatusException conflict(String message) {
		return new ResponseStatusException(Status.CONFLICT_409, message);
	}

	public void send(ServerResponse res) {
		String message = getMessage();
		if (message == null) {
			res.status(status).send();
		} else {
			ControllerSupport.sendError(res, status, message);
		}
	}
}
