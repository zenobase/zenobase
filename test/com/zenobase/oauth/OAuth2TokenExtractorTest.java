package com.zenobase.oauth;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

public class OAuth2TokenExtractorTest {

	@Test
	public void testSimpleToken() {
		ExpiringToken token = new OAuth2TokenExtractor().extract("{ \"access_token\" : \"e16b2f39\" }");
		assertThat(token.getToken()).as("token").isEqualTo("e16b2f39");
		assertThat(token.getSecret()).as("secret").isEmpty();
		assertThat(token.getRefreshToken()).as("refresh token").isNull();
		assertThat(token.getExpires()).as("expiration date").isNull();
		assertThat(token.isExpired()).as("expired").isFalse();
	}

	@Test
	public void testExpiringToken() {
		ExpiringToken token = new OAuth2TokenExtractor().extract("{ \"access_token\" : \"e16b2f39\", \"refresh_token\" : \"888909ec\", \"expires_in\" : 10800 }");
		assertThat(token.getToken()).as("token").isEqualTo("e16b2f39");
		assertThat(token.getSecret()).as("secret").isEmpty();
		assertThat(token.getRefreshToken()).as("refresh token").isEqualTo("888909ec");
		assertThat(token.getExpires().isAfterNow()).as("expiration date is in the future " + token.getExpires()).isTrue();
		assertThat(token.isExpired()).as("expired").isFalse();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInvalidToken() {
		 new OAuth2TokenExtractor().extract("{}");
	}
}
