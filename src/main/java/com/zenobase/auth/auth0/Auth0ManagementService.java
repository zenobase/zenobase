package com.zenobase.auth.auth0;

import com.auth0.client.mgmt.ManagementApi;
import com.auth0.client.mgmt.types.AuthenticationMethodTypeEnum;
import com.auth0.client.mgmt.types.GetClientResponseContent;
import com.auth0.client.mgmt.types.ListUsersRequestParameters;
import com.auth0.client.mgmt.types.SearchEngineVersionsEnum;
import com.auth0.client.mgmt.types.UpdateUserRequestContent;
import com.zenobase.auth.IdentityProvider;
import com.zenobase.auth.Passkey;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Auth0ManagementService implements IdentityProvider {

	private static final Logger logger = LoggerFactory.getLogger(Auth0ManagementService.class);

	private final ManagementApi client;

	@Inject
	public Auth0ManagementService(
		@Named("auth0.domain") String domain,
		@Named("auth0.m2m.domain") String m2mDomain,
		@Named("auth0.m2m.client_id") String clientId,
		@Named("auth0.m2m.client_secret") String clientSecret
	) {
		// The Management API only accepts tokens whose audience is the *canonical* Auth0 tenant
		// (https://<tenant>/api/v2/) — custom domains are not allowed for M2M-to-Management API.
		// Prefer `auth0.m2m.domain` (canonical) when set; otherwise fall back to `auth0.domain`
		// (which is fine when it's already the canonical tenant). The SDK's .domain(...) wants a
		// bare hostname and prepends https:// itself, so strip any scheme our config carries.
		String host = (m2mDomain.isEmpty() ? domain : m2mDomain).replaceFirst("^https?://", "");
		this.client = ManagementApi.builder().domain(host).clientCredentials(clientId, clientSecret).build();
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
			logger.error("Failed to update Auth0 email for user {}", externalId, e);
		}
	}

	@Override
	public void deleteUser(User user) {
		String externalId = user.getExternalId();
		if (externalId == null) {
			// external_id is back-filled on Auth0 login (see Auth0UserSynchronizer), but legacy
			// users who haven't logged in since that wired up still have it null locally even
			// though they exist in Auth0. Fall back to a username lookup so we don't leave the
			// Auth0 record behind.
			externalId = lookupExternalIdByUsername(user.getName());
		}
		if (externalId == null) {
			return;
		}
		try {
			client.users().delete(externalId);
			logger.info("Deleted Auth0 user {}", externalId);
		} catch (Exception e) {
			logger.error("Failed to delete Auth0 user {}", externalId, e);
		}
	}

	private @Nullable String lookupExternalIdByUsername(@Nullable String username) {
		if (username == null) {
			return null;
		}
		try {
			var params = ListUsersRequestParameters.builder()
				.q("username:\"" + username + "\"")
				.searchEngine(SearchEngineVersionsEnum.V3)
				.perPage(2)
				.build();
			var iter = client.users().list(params).iterator();
			if (!iter.hasNext()) {
				return null;
			}
			var first = iter.next();
			if (iter.hasNext()) {
				logger.error("Multiple Auth0 users found for username {}; not deleting", username);
				return null;
			}
			return first.getUserId().orElse(null);
		} catch (Exception e) {
			logger.error("Failed to look up Auth0 user by username {}", username, e);
			return null;
		}
	}

	@Override
	public List<Passkey> listPasskeys(User user) {
		String externalId = user.getExternalId();
		if (externalId == null) {
			return List.of();
		}
		try {
			List<Passkey> passkeys = new ArrayList<>();
			client
				.users()
				.authenticationMethods()
				.list(externalId)
				.forEach(method -> {
					if (AuthenticationMethodTypeEnum.PASSKEY.equals(method.getType())) {
						passkeys.add(
							new Passkey(
								method.getId(),
								method.getName().orElse(null),
								method.getCreatedAt().toString(),
								method.getLastAuthAt().map(OffsetDateTime::toString).orElse(null),
								method.getUserAgent().orElse(null)
							)
						);
					}
				});
			return passkeys;
		} catch (Exception e) {
			logger.error("Failed to list Auth0 passkeys for user {}", externalId, e);
			return List.of();
		}
	}

	@Override
	public void deletePasskey(User user, String passkeyId) {
		String externalId = user.getExternalId();
		if (externalId == null) {
			return;
		}
		var method = client.users().authenticationMethods().get(externalId, passkeyId);
		if (!AuthenticationMethodTypeEnum.PASSKEY.equals(method.getType())) {
			throw new IllegalArgumentException("authentication method is not a passkey");
		}
		client.users().authenticationMethods().delete(externalId, passkeyId);
		logger.info("Deleted Auth0 passkey {} for user {}", passkeyId, externalId);
	}

	@Override
	public @Nullable String getApplicationName(Identity application) {
		try {
			return client.clients().get(application.id()).getName().orElse(null);
		} catch (Exception e) {
			logger.warn("Failed to look up Auth0 application name for {}", application.id(), e);
			return null;
		}
	}

	@Override
	public void deleteApplication(Identity application) {
		// Defense-in-depth against {@link com.zenobase.controllers.ExternalClientController#revoke} or any future
		// caller reaching us with a first-party {@code client_id} (the SPA, the M2M client itself, manually-registered
		// partner apps). Auth0 sets {@code is_first_party=false} on DCR-registered Applications and {@code true} on
		// manually-registered ones, so we GET the Application and refuse unless the flag is explicitly false. Fail
		// safe if the GET errors or the flag is absent — better a logged refusal than a nuked production app.
		try {
			GetClientResponseContent response = client.clients().get(application.id());
			if (!isExternalApplication(response.getIsFirstParty())) {
				logger.error(
					"Refusing to delete Auth0 application {} (is_first_party={})",
					application.id(),
					response.getIsFirstParty().orElse(null)
				);
				return;
			}
			client.clients().delete(application.id());
			logger.info("Deleted Auth0 application {}", application.id());
		} catch (Exception e) {
			logger.error("Failed to delete Auth0 application {}", application.id(), e);
		}
	}

	/**
	 * Pure, package-private for testing. Returns true only when {@code is_first_party} is explicitly {@code false} —
	 * an absent flag is treated as first-party so an SDK regression or unexpected response shape can't lead us to
	 * delete a production Application.
	 */
	static boolean isExternalApplication(Optional<Boolean> firstPartyFlag) {
		return firstPartyFlag.equals(Optional.of(false));
	}
}
