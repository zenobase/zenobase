package com.zenobase.controllers;

import org.fest.assertions.Assertions;
import org.fest.assertions.StringAssert;
import org.junit.Test;

public class AuthorizationContextTest {

	@Test
	public void test() {
		assertThat("Bearer 1234567890abcdefg").isEqualTo("1234567890abcdefg");
		assertThat(null).isNull();
		assertThat("Basic xyz").isNull();
		assertThat("Bearer ").isNull();
	}

	private StringAssert assertThat(String header) {
		return Assertions.assertThat(AuthorizationContext.extractToken(header));
	}
}
