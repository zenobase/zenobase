package com.zenobase.auth.auth0;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Construction-only tests — the rest of {@link Auth0ManagementService} would require a real Auth0 tenant or a mock
 * Management API client. {@code protected_client_ids} parsing is pure and worth pinning down: a regression here is
 * how the SPA / M2M Application would get silently nuked.
 */
public class Auth0ManagementServiceTest {

	private static final String DOMAIN = "tenant.auth0.com";
	private static final String M2M_DOMAIN = "";
	private static final String M2M_CLIENT_ID = "m2m-client-id";
	private static final String M2M_CLIENT_SECRET = "m2m-secret";

	@Test
	public void m2mClientIdIsAlwaysProtected() {
		Auth0ManagementService service = new Auth0ManagementService(
			DOMAIN,
			M2M_DOMAIN,
			M2M_CLIENT_ID,
			M2M_CLIENT_SECRET,
			""
		);
		assertThat(service.protectedClientIds()).containsExactly(M2M_CLIENT_ID);
	}

	@Test
	public void configuredProtectedClientIdsAreAdded() {
		Auth0ManagementService service = new Auth0ManagementService(
			DOMAIN,
			M2M_DOMAIN,
			M2M_CLIENT_ID,
			M2M_CLIENT_SECRET,
			"spa-client-id, partner-app-id"
		);
		assertThat(service.protectedClientIds()).containsExactlyInAnyOrder(
			M2M_CLIENT_ID,
			"spa-client-id",
			"partner-app-id"
		);
	}

	@Test
	public void emptyAndWhitespaceEntriesAreIgnored() {
		Auth0ManagementService service = new Auth0ManagementService(
			DOMAIN,
			M2M_DOMAIN,
			M2M_CLIENT_ID,
			M2M_CLIENT_SECRET,
			" , spa-client-id ,, , "
		);
		assertThat(service.protectedClientIds()).containsExactlyInAnyOrder(M2M_CLIENT_ID, "spa-client-id");
	}
}
