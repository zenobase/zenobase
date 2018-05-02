package com.zenobase.common;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

public class BCryptTest {

	@Test
	public void test() {

		String password = "abc";
		String hashed = BCrypt.hashpw(password);
		String rehashed = BCrypt.hashpw(password);

		assertThat(BCrypt.checkpw(password, hashed)).as("password matches first hash").isTrue();
		assertThat(BCrypt.checkpw(password, hashed)).as("password matches second hash").isTrue();
		assertThat(hashed.equals(rehashed)).as("hashes are equal").isFalse();
		assertThat(BCrypt.checkpw(password.toUpperCase(), hashed)).as("invalid password matches hash").isFalse();
	}
}
