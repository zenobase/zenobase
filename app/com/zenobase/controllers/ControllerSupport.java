package com.zenobase.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import play.mvc.Controller;
import play.mvc.With;

import com.zenobase.actions.Gatekeeper;
import com.zenobase.actions.NoCache;
import com.zenobase.actions.QuotaExceptionHandler;
import com.zenobase.json.Nodes;
import com.zenobase.oauth.Authorization;

@With({ Gatekeeper.class, NoCache.class, QuotaExceptionHandler.class })
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
		return status(status, Nodes.newObject("message", sanitize(Objects.firstNonNull(message, "?"))));
	}

	private static String sanitize(String message) {
		return message.contains("<") ? Jsoup.clean(message, Whitelist.basic()) : message;
	}
}
