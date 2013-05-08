package com.zenobase.controllers;

import org.fest.assertions.Assertions;
import org.fest.assertions.BooleanAssert;
import org.junit.Test;

import com.zenobase.models.User;

public class OAuthRedirectValidatorTest {

	@Test
	public void test() {
		User client = new User("tester");
		client.setEmail("me@here.test");
		assertThat(client, "http://here.test/callback").isTrue();
		assertThat(client, "http://www.here.test/callback").isTrue();
		assertThat(client, "https://here.test/callback").isTrue();
		assertThat(client, "https://elsewhere.test/callback").isFalse();
		assertThat(client, "x-oauth-foo://callback").isTrue();
	}

	private static BooleanAssert assertThat(User client, String uri) {
		OAuthRedirectValidator validator = new OAuthRedirectValidator(client);
		return Assertions.assertThat(validator.valid(uri)).as(uri);
	}
}
