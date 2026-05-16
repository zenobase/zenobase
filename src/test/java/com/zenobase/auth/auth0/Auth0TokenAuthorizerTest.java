package com.zenobase.auth.auth0;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Tests the pure scope-mapping behavior introduced for multi-audience tokens. The full auth pipeline (token →
 * synchronizer → Authorization) is exercised by integration tests; this just locks in the audience-to-scope mapping.
 */
public class Auth0TokenAuthorizerTest {

	private static final String FIRST_PARTY = "https://api.zenobase.com";
	private static final String EXTERNAL = "https://api.zenobase.com/external";

	@Test
	public void testFirstPartyAudienceProducesNullScope() {
		assertThat(Auth0TokenAuthorizer.scopeFor(FIRST_PARTY, EXTERNAL)).isNull();
	}

	@Test
	public void testExternalAudienceProducesExternalScope() {
		assertThat(Auth0TokenAuthorizer.scopeFor(EXTERNAL, EXTERNAL)).isEqualTo(Auth0TokenAuthorizer.EXTERNAL_SCOPE);
	}

	@Test
	public void testUnknownAudienceProducesNullScope() {
		assertThat(Auth0TokenAuthorizer.scopeFor("https://attacker.example.com", EXTERNAL)).isNull();
	}

	@Test
	public void testNullAudienceProducesNullScope() {
		assertThat(Auth0TokenAuthorizer.scopeFor(null, EXTERNAL)).isNull();
	}

	@Test
	public void testExternalAudienceNotConfiguredAlwaysFirstParty() {
		assertThat(Auth0TokenAuthorizer.scopeFor(EXTERNAL, null)).isNull();
		assertThat(Auth0TokenAuthorizer.scopeFor(FIRST_PARTY, null)).isNull();
	}
}
