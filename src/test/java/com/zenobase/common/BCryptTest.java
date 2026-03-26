package com.zenobase.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

public class BCryptTest {

	@Test
	public void test() {

		String password = "abc";
		String hashed = BCrypt.hashpw(password);
		String rehashed = BCrypt.hashpw(password);

		assertThat(BCrypt.checkpw(password, hashed))
				.as("password matches first hash")
				.isTrue();
		assertThat(BCrypt.checkpw(password, hashed))
				.as("password matches second hash")
				.isTrue();
		assertThat(hashed.equals(rehashed)).as("hashes are equal").isFalse();
		assertThat(BCrypt.checkpw(password.toUpperCase(), hashed))
				.as("invalid password matches hash")
				.isFalse();
	}
}
