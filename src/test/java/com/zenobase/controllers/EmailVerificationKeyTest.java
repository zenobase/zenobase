package com.zenobase.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class EmailVerificationKeyTest {

	private final EmailVerificationKey key = new EmailVerificationKey("jdoe", "jdoe@zenobase.com");

	@Test
	public void testValidKey() {
		assertThat(new EmailVerificationKey("jdoe", "jdoe@zenobase.com").validate(key.getKey()))
				.isTrue();
	}

	@Test
	public void testInvalidKey() {
		assertThat(new EmailVerificationKey("jdoe", "jdoe@zenobase.org").validate(key.getKey()))
				.isFalse();
	}

	@Test
	public void testBogusKey() {
		assertThat(key.validate("x")).isFalse();
	}
}
