package com.zenobase.auth.auth0;

import com.auth0.client.mgmt.ManagementApi;
import com.auth0.client.mgmt.types.SessionDate;
import com.auth0.client.mgmt.types.SessionDeviceMetadata;
import com.auth0.client.mgmt.types.SessionResponseContent;
import com.auth0.client.mgmt.types.UpdateUserRequestContent;
import com.google.common.annotations.VisibleForTesting;
import com.zenobase.auth.UserDirectory;
import com.zenobase.models.Session;
import com.zenobase.models.User;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Auth0ManagementService implements UserDirectory {

	private static final Logger logger = LoggerFactory.getLogger(Auth0ManagementService.class);

	private final ManagementApi client;

	@Inject
	public Auth0ManagementService(
		@Named("auth0.domain") String domain,
		@Named("auth0.m2m.client_id") String clientId,
		@Named("auth0.m2m.client_secret") String clientSecret
	) {
		this.client = ManagementApi.builder().domain(domain).clientCredentials(clientId, clientSecret).build();
	}

	@VisibleForTesting
	Auth0ManagementService(ManagementApi client) {
		this.client = client;
	}

	@Override
	public void updateEmail(User user, String newEmail) {
		String externalId = user.getExternalId();
		if (externalId == null) {
			return;
		}
		try {
			client
				.users()
				.update(externalId, UpdateUserRequestContent.builder().email(newEmail).emailVerified(false).build());
			logger.info("Updated Auth0 email for user {} to {}", externalId, newEmail);
		} catch (Exception e) {
			logger.warn("Failed to update Auth0 email for user {}: {}", externalId, e.getMessage());
		}
	}

	@Override
	public void deleteUser(User user) {
		String externalId = user.getExternalId();
		if (externalId == null) {
			return;
		}
		try {
			client.users().delete(externalId);
			logger.info("Deleted Auth0 user {}", externalId);
		} catch (Exception e) {
			logger.warn("Failed to delete Auth0 user {}: {}", externalId, e.getMessage());
		}
	}

	/**
	 * Lists the Auth0 sessions for a user.
	 */
	@Override
	public List<Session> listSessions(String externalId) {
		List<Session> result = new ArrayList<>();
		for (SessionResponseContent s : client.users().sessions().list(externalId)) {
			result.add(toSession(s));
		}
		return result;
	}

	/**
	 * Revokes a single Auth0 session by id. Verifies the session belongs to the given user
	 * first (IDOR defense). Returns {@code true} if a revoke was issued; {@code false} if the
	 * session is not owned by that user. Uses {@code sessions().revoke} (not
	 * {@code sessions().delete}), which revokes the session's refresh token as well.
	 */
	@Override
	public boolean revokeSession(String externalId, String sessionId) {
		boolean owned = false;
		for (SessionResponseContent s : client.users().sessions().list(externalId)) {
			if (sessionId.equals(s.getId().orElse(null))) {
				owned = true;
				break;
			}
		}
		if (!owned) {
			return false;
		}
		client.sessions().revoke(sessionId);
		logger.info("Revoked Auth0 session {} for user {}", sessionId, externalId);
		return true;
	}

	/**
	 * Revokes all Auth0 sessions and refresh tokens for a user in one call (including orphan
	 * refresh tokens not bound to a visible session). Used by the admin "Revoke all sessions"
	 * action.
	 */
	@Override
	public boolean revokeAllSessions(String externalId) {
		client.users().revokeAccess(externalId);
		logger.info("Revoked all Auth0 sessions and refresh tokens for user {}", externalId);
		return true;
	}

	private static Session toSession(SessionResponseContent s) {
		String id = s.getId().orElse("");
		String userAgent = null;
		String ip = null;
		Optional<SessionDeviceMetadata> device = s.getDevice();
		if (device.isPresent()) {
			SessionDeviceMetadata d = device.get();
			userAgent = d.getLastUserAgent().orElse(d.getInitialUserAgent().orElse(null));
			String lastIp = d.getLastIp().orElse(null);
			ip = lastIp != null ? lastIp : d.getInitialIp().orElse(null);
		}
		String createdAt = toIso(s.getCreatedAt());
		String lastActiveAt = toIso(s.getLastInteractedAt());
		if (lastActiveAt == null) {
			lastActiveAt = toIso(s.getUpdatedAt());
		}
		return Session.of(id, userAgent, ip, createdAt, lastActiveAt);
	}

	private static @Nullable String toIso(Optional<SessionDate> opt) {
		return opt
			.map(d ->
				d.visit(
					new SessionDate.Visitor<@Nullable String>() {
						@Override
						public @Nullable String visit(OffsetDateTime v) {
							return v.toString();
						}

						@Override
						public @Nullable String visit(Map<String, Object> v) {
							return null;
						}
					}
				)
			)
			.orElse(null);
	}
}
