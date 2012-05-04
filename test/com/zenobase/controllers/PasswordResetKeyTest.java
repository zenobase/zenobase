package com.zenobase.controllers;

import static org.fest.assertions.Assertions.assertThat;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.zenobase.models.User;

public class PasswordResetKeyTest {

	private final User user = new User("jdoe");

	@Before
	public void setUp() {
		user.setPassword("secret123");
	}

	@Test
	public void testValidKey() {
		PasswordResetKey key = new PasswordResetKey(user);
		assertThat(new PasswordResetKey(user, key.getExpirationToken()).validate(key.getKey())).isTrue();
	}

	@Test
	public void testInvalidKey() {
		PasswordResetKey key = new PasswordResetKey(user);
		User other = user.copy();
		user.setPassword("123secret");
		assertThat(new PasswordResetKey(other, key.getExpirationToken()).validate(key.getKey())).isFalse();
	}

	@Test
	public void testExpiredKey() {
		PasswordResetKey key = new PasswordResetKey(user, new DateTime().minusHours(1));
		assertThat(new PasswordResetKey(user, key.getExpirationToken()).validate(key.getKey())).isFalse();
	}

	@Test
	public void testBogusKey() {
		assertThat(new PasswordResetKey(user).validate("x")).isFalse();
	}
}
