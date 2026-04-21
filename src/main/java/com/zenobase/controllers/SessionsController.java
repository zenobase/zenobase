package com.zenobase.controllers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.auth.UserDirectory;
import com.zenobase.json.Nodes;
import com.zenobase.models.Session;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.UserLookup;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import java.util.List;
import org.jspecify.annotations.Nullable;

public class SessionsController extends ControllerSupport {

	private final UserDirectory directory;
	private final UserRepository users;

	@Inject
	public SessionsController(AuthorizationContext security, UserDirectory directory, UserRepository users) {
		super(security);
		this.directory = directory;
		this.users = users;
	}

	/** {@code GET /users/{userId}/sessions/} — self or superuser. */
	public void findByUser(ServerRequest req, ServerResponse res) {
		String userId = req.path().pathParameters().get("userId");
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		if (auth.getScope() != null) {
			sendForbidden(res);
			return;
		}
		User user = new UserLookup(users).getUser(userId);
		if (user == null || user.getName() == null) {
			sendNotFound(res, "user not found");
			return;
		}
		if (!user.is(auth.getPrincipal()) && !users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		String externalId = user.getExternalId();
		List<Session> sessions = externalId != null ? directory.listSessions(externalId) : List.of();
		sendOk(res, buildSelfSessionList(sessions, auth.getSessionId()));
	}

	/** {@code DELETE /users/{userId}/sessions/{sessionId}} — self or superuser. */
	public void deleteOne(ServerRequest req, ServerResponse res) {
		String userId = req.path().pathParameters().get("userId");
		String sessionId = req.path().pathParameters().get("sessionId");
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		if (auth.getScope() != null) {
			sendForbidden(res);
			return;
		}
		User user = new UserLookup(users).getUser(userId);
		if (user == null || user.getName() == null) {
			sendNotFound(res, "user not found");
			return;
		}
		if (!user.is(auth.getPrincipal()) && !users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		String externalId = user.getExternalId();
		if (externalId == null) {
			sendNotFound(res, "session not found");
			return;
		}
		boolean revoked = directory.revokeSession(externalId, sessionId);
		if (!revoked) {
			sendNotFound(res, "session not found");
			return;
		}
		sendNoContent(res);
	}

	/** {@code DELETE /users/{userId}/sessions/} — superuser only (admin bulk revoke). */
	public void deleteAll(ServerRequest req, ServerResponse res) {
		String userId = req.path().pathParameters().get("userId");
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		if (auth.getScope() != null || !users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		User user = new UserLookup(users).getUser(userId);
		if (user == null || user.getName() == null) {
			sendNotFound(res, "user not found");
			return;
		}
		String externalId = user.getExternalId();
		if (externalId != null) {
			directory.revokeAllSessions(externalId);
		}
		sendNoContent(res);
	}

	/** {@code GET /sessions/?user=&offset=&limit=} — superuser only (admin listing, filtered by user). */
	public void findAll(ServerRequest req, ServerResponse res) {
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		if (auth.getScope() != null || !users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		String userId = req.query().first("user").orElse("");
		int offset;
		int limit;
		try {
			offset = Integer.parseInt(req.query().first("offset").orElse("0"));
			limit = Integer.parseInt(req.query().first("limit").orElse("10"));
		} catch (NumberFormatException e) {
			sendBadRequest(res, "invalid offset/limit");
			return;
		}
		if (offset < 0 || offset > 1000) {
			sendBadRequest(res, "expected offset in [0..1000]");
			return;
		}
		if (limit < 0 || limit > 100) {
			sendBadRequest(res, "expected limit in [0..100]");
			return;
		}
		if (userId.isEmpty()) {
			// Auth0 Management API has no global "list all sessions" endpoint, so the admin
			// endpoint requires a user filter. Return an empty page when unfiltered.
			sendOk(res, buildAdminEmpty());
			return;
		}
		User user = new UserLookup(users).getUser(userId);
		if (user == null || user.getName() == null) {
			sendOk(res, buildAdminEmpty());
			return;
		}
		String externalId = user.getExternalId();
		List<Session> all = externalId != null ? directory.listSessions(externalId) : List.of();
		int total = all.size();
		int from = Math.min(offset, total);
		int to = Math.min(from + limit, total);
		List<Session> page = all.subList(from, to);
		ObjectNode body = Nodes.newObject();
		body.put("total", total);
		ArrayNode arr = body.putArray("sessions");
		for (Session s : page) {
			arr.add(s.withOwner(user.getId(), user.getName()).toJson());
		}
		sendOk(res, body);
	}

	private static ObjectNode buildSelfSessionList(List<Session> sessions, @Nullable String currentSid) {
		ObjectNode body = Nodes.newObject();
		ArrayNode arr = body.putArray("sessions");
		for (Session s : sessions) {
			arr.add(s.withCurrent(currentSid != null && currentSid.equals(s.id())).toJson());
		}
		return body;
	}

	private static ObjectNode buildAdminEmpty() {
		ObjectNode body = Nodes.newObject();
		body.put("total", 0);
		body.putArray("sessions");
		return body;
	}
}
