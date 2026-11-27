package com.zenobase.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.zenobase.auth.UserStateCache;
import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.UserRepository;
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

	protected static boolean isFirstPartySuperuser(UserRepository users, Authorization auth) {
		return auth.getScope() == null && users.isSuperuser(auth.getPrincipal());
	}

	// Guards: each throws a ResponseStatusException, which writes the same response as the matching sendX helper.

	protected Authorization requireAuthorization(ServerRequest req) {
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			throw ResponseStatusException.unauthorized();
		}
		return auth;
	}

	/** Rejects tokens issued to external clients (MCP, third-party integrations). */
	protected static void requireFirstParty(Authorization auth) {
		if (auth.getScope() != null) {
			throw ResponseStatusException.forbidden();
		}
	}

	protected static void requireFirstPartySuperuser(UserRepository users, Authorization auth) {
		requirePermission(isFirstPartySuperuser(users, auth));
	}

	protected static void requireSelfOrSuperuser(UserRepository users, Authorization auth, Identity principal) {
		requirePermission(auth.getPrincipal().equals(principal) || users.isSuperuser(auth.getPrincipal()));
	}

	protected static void requirePermission(boolean permitted) {
		if (!permitted) {
			throw ResponseStatusException.forbidden();
		}
	}

	/** Anonymous callers get a 401 rather than a 403, so they know to sign in. */
	protected static void requireRole(Bucket bucket, @Nullable Authorization auth, Role role) {
		if (!bucket.hasRole(auth, role)) {
			throw auth == null ? ResponseStatusException.unauthorized() : ResponseStatusException.forbidden();
		}
	}

	protected static void requireNotArchived(Bucket bucket) {
		if (bucket.isArchived()) {
			throw ResponseStatusException.conflict("bucket is archived");
		}
	}

	protected void requireNotSuspended(Authorization auth) {
		if (authContext.userState(auth.getPrincipal()) == UserStateCache.UserState.SUSPENDED) {
			throw ResponseStatusException.forbidden("account is suspended");
		}
	}

	protected static <T> T requireFound(@Nullable T value) {
		if (value == null) {
			throw ResponseStatusException.notFound();
		}
		return value;
	}

	protected static <T> T requireFound(@Nullable T value, String message) {
		if (value == null) {
			throw ResponseStatusException.notFound(message);
		}
		return value;
	}

	protected static void requirePaging(int offset, int limit) {
		if (offset < 0 || offset > 1000) {
			throw ResponseStatusException.badRequest("expected offset in [0..1000]");
		}
		if (limit < 0 || limit > 100) {
			throw ResponseStatusException.badRequest("expected limit in [0..100]");
		}
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

	// Error response helpers for filters and routing error handlers; controllers throw ResponseStatusException

	public static void sendForbidden(ServerResponse res, String message) {
		sendError(res, Status.FORBIDDEN_403, message);
	}

	public static void sendInternalServerError(ServerResponse res, String message) {
		sendError(res, Status.INTERNAL_SERVER_ERROR_500, message);
	}

	public static void sendServiceUnavailable(ServerResponse res, String message) {
		sendError(res, Status.SERVICE_UNAVAILABLE_503, message);
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
