package com.zenobase.controllers;

import org.assertj.core.api.Assertions;

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
		assertThat(client, "foo://").isFalse(); // debatable
		assertThat(client, "x-foo://callback").isTrue();
		assertThat(client, "x-foo://").isFalse(); // debatable
		assertThat(client, "x-foo:///").isTrue();
		assertThat(client, "foo").isFalse();
	}

	private static org.assertj.core.api.AbstractBooleanAssert<?> assertThat(User client, String uri) {
		OAuthRedirectValidator validator = new OAuthRedirectValidator(client);
		return Assertions.assertThat(validator.valid(uri)).as(uri);
	}
}
