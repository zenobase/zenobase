package com.zenobase.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zenobase.models.User;

public class PasswordResetKeyTest {

	private final User user = new User("jdoe");

	@BeforeEach
	public void setUp() {
		user.setPassword("secret123");
	}

	@Test
	public void testValidKey() {
		PasswordResetKey key = new PasswordResetKey(user);
		assertThat(new PasswordResetKey(user, key.getExpirationToken()).validate(key.getKey()))
				.isTrue();
	}

	@Test
	public void testInvalidKey() {
		PasswordResetKey key = new PasswordResetKey(user);
		User other = user.copy();
		user.setPassword("123secret");
		assertThat(new PasswordResetKey(other, key.getExpirationToken()).validate(key.getKey()))
				.isFalse();
	}

	@Test
	public void testExpiredKey() {
		PasswordResetKey key = new PasswordResetKey(user, DateTime.now().minusHours(1));
		assertThat(new PasswordResetKey(user, key.getExpirationToken()).validate(key.getKey()))
				.isFalse();
	}

	@Test
	public void testBogusKey() {
		assertThat(new PasswordResetKey(user).validate("x")).isFalse();
	}
}
