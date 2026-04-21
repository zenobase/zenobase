package com.zenobase.models;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.Nodes;
import org.jspecify.annotations.Nullable;

/**
 * DTO for an Auth0 session, as surfaced by GET /users/{userId}/sessions/ and
 * GET /sessions/. Dates are ISO-8601 strings; {@code ip} and {@code lastActiveAt}
 * may be {@code null}. The {@code current} flag is set by the controller only when
 * the caller's {@code sid} claim matches this session's id (self-view). Admin-scoped
 * rows additionally carry {@code userId} and {@code username}.
 */
public record Session(
	String id,
	@Nullable String userAgent,
	@Nullable String ip,
	@Nullable String createdAt,
	@Nullable String lastActiveAt,
	boolean current,
	@Nullable String userId,
	@Nullable String username
) {
	public static Session of(
		String id,
		@Nullable String userAgent,
		@Nullable String ip,
		@Nullable String createdAt,
		@Nullable String lastActiveAt
	) {
		return new Session(id, userAgent, ip, createdAt, lastActiveAt, false, null, null);
	}

	public Session withCurrent(boolean current) {
		return new Session(id, userAgent, ip, createdAt, lastActiveAt, current, userId, username);
	}

	public Session withOwner(@Nullable String userId, @Nullable String username) {
		return new Session(id, userAgent, ip, createdAt, lastActiveAt, current, userId, username);
	}

	public ObjectNode toJson() {
		ObjectNode node = Nodes.newObject();
		node.put("id", id);
		node.put("userAgent", userAgent);
		node.put("ip", ip);
		node.put("createdAt", createdAt);
		node.put("lastActiveAt", lastActiveAt);
		node.put("current", current);
		if (userId != null) {
			node.put("userId", userId);
		}
		if (username != null) {
			node.put("username", username);
		}
		return node;
	}
}
