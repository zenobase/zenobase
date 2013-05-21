package com.zenobase.controllers;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import play.mvc.Controller;
import play.mvc.With;

import com.zenobase.actions.NoCache;
import com.zenobase.actions.QuotaExceptionHandler;
import com.zenobase.json.Nodes;
import com.zenobase.oauth.Authorization;

@With({NoCache.class, QuotaExceptionHandler.class})
public abstract class ControllerSupport extends Controller implements CustomHeaders {

	private final AuthorizationContext authContext;

	protected ControllerSupport(AuthorizationContext authContext) {
		this.authContext = authContext;
	}

	protected Authorization getCurrentAuthorization() {
		return authContext.current();
	}

	protected static ObjectNode body() {
		ObjectNode node = body(ObjectNode.class);
		return node != null ? node : Nodes.newObject();
	}

	protected static <T extends JsonNode> T body(Class<T> type) {
		JsonNode node = request().body().asJson();
		return type.isInstance(node) ? type.cast(node) : null;
	}

	public static Status badRequest(String message) {
		return result(BAD_REQUEST, message);
	}

	public static Status unauthorized(String message) {
		return result(UNAUTHORIZED, message);
	}

	public static Status forbidden(String message) {
		return result(FORBIDDEN, message);
	}

	public static Status notFound(String message) {
		return result(NOT_FOUND, message);
	}

	public static Status conflict(String message) {
		return result(CONFLICT, message);
	}

	public static Status internalServerError(String message) {
		return result(INTERNAL_SERVER_ERROR, message);
	}

	private static Status result(int status, String message) {
		return status(status, Nodes.newObject("message", message));
	}
}
