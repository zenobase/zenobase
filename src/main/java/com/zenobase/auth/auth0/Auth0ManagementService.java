package com.zenobase.auth.auth0;

import com.auth0.client.mgmt.ManagementApi;
import com.auth0.client.mgmt.types.UpdateUserRequestContent;
import com.zenobase.auth.UserDirectory;
import com.zenobase.models.User;
import jakarta.inject.Inject;
import jakarta.inject.Named;
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
}
