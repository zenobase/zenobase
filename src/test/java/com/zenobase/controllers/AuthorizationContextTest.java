package com.zenobase.controllers;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class AuthorizationContextTest {

	@Test
	public void test() {
		assertThat("Bearer 1234567890abcdefg").isEqualTo("1234567890abcdefg");
		assertThat(null).isNull();
		assertThat("Basic xyz").isNull();
		assertThat("Bearer ").isNull();
	}

	private org.assertj.core.api.AbstractStringAssert<?> assertThat(String header) {
		return Assertions.assertThat(AuthorizationContext.extractToken(header));
	}
}
