package com.zenobase.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class BCryptTest {

	@Test
	public void test() {

		String password = "abc";
		String hashed = BCryptUtils.hashpw(password);
		String rehashed = BCryptUtils.hashpw(password);

		assertThat(hashed).doesNotContain(".");
		assertThat(BCryptUtils.checkpw(password, hashed))
				.as("password matches first hash")
				.isTrue();
		assertThat(BCryptUtils.checkpw(password, rehashed))
				.as("password matches second hash")
				.isTrue();
		assertThat(hashed.equals(rehashed)).as("hashes are equal").isFalse();
		assertThat(BCryptUtils.checkpw(password.toUpperCase(), hashed))
				.as("invalid password matches hash")
				.isFalse();
	}
}
