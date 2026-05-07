package com.zenobase.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.zenobase.auth.UserStateCache;
import com.zenobase.json.Nodes;
import com.zenobase.oauth.Authorization;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.jspecify.annotations.Nullable;

public abstract class ControllerSupport implements CustomHeaders {

	protected static final String LOCATION = "Location";

	private final AuthorizationContext authContext;

	protected ControllerSupport(AuthorizationContext authContext) {
		this.authContext = authContext;
	}

	protected @Nullable Authorization getCurrentAuthorization(ServerRequest req) {
		return authContext.current(req);
	}

	protected boolean sendForbiddenIfSuspended(Authorization auth, ServerResponse res) {
		if (authContext.userState(auth.getPrincipal()) == UserStateCache.UserState.SUSPENDED) {
			sendForbidden(res, "account is suspended");
			return true;
		}
		return false;
	}

	protected static ObjectNode body(ServerRequest req) {
		ObjectNode node = body(req, ObjectNode.class);
		return node != null ? node : Nodes.newObject();
	}

	protected static <T extends JsonNode> @Nullable T body(ServerRequest req, Class<T> type) {
		JsonNode node = req.content().as(JsonNode.class);
		return type.isInstance(node) ? type.cast(node) : null;
	}

	// Response helpers

	protected static void sendOk(ServerResponse res, JsonNode json) {
		res.send(json);
	}

	protected static void sendOk(ServerResponse res) {
		res.status(Status.OK_200).send();
	}

	protected static void sendCreated(ServerResponse res, JsonNode json) {
		res.status(Status.CREATED_201).send(json);
	}

	protected static void sendNoContent(ServerResponse res) {
		res.status(Status.NO_CONTENT_204).send();
	}

	protected static void sendNotFound(ServerResponse res) {
		res.status(Status.NOT_FOUND_404).send();
	}

	protected static void sendUnauthorized(ServerResponse res) {
		res.status(Status.UNAUTHORIZED_401).send();
	}

	protected static void sendForbidden(ServerResponse res) {
		res.status(Status.FORBIDDEN_403).send();
	}

	protected static void sendRedirect(ServerResponse res, String url) {
		res.status(Status.FOUND_302);
		res.header(HeaderNames.LOCATION, url);
		res.send();
	}

	protected static void sendStatus(ServerResponse res, int statusCode, JsonNode json) {
		res.status(Status.create(statusCode)).send(json);
	}

	// Error response helpers (also used by filters)

	public static void sendBadRequest(ServerResponse res, String message) {
		sendError(res, Status.BAD_REQUEST_400, message);
	}

	public static void sendBadRequest(ServerResponse res, JsonNode json) {
		res.status(Status.BAD_REQUEST_400).send(json);
	}

	public static void sendForbidden(ServerResponse res, String message) {
		sendError(res, Status.FORBIDDEN_403, message);
	}

	public static void sendNotFound(ServerResponse res, String message) {
		sendError(res, Status.NOT_FOUND_404, message);
	}

	public static void sendConflict(ServerResponse res, String message) {
		sendError(res, Status.CONFLICT_409, message);
	}

	public static void sendInternalServerError(ServerResponse res, String message) {
		sendError(res, Status.INTERNAL_SERVER_ERROR_500, message);
	}

	public static void sendError(ServerResponse res, Status status, String message) {
		res.status(status);
		res.header(HeaderNames.CONTENT_TYPE, "application/json");
		res.send(Nodes.newObject("message", sanitize(MoreObjects.firstNonNull(message, "?"))).toString());
	}

	private static String sanitize(String message) {
		return message.contains("<") ? Jsoup.clean(message, Safelist.basic()) : message;
	}

	// Header helpers

	protected static void setHeader(ServerResponse res, String name, @Nullable String value) {
		if (value != null) {
			res.header(HeaderNames.create(name), value);
		}
	}
}
