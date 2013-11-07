package com.zenobase.services;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

public class QuotaTest {

	@Test
	public void test() {
		Quota quota = new Quota(100, 20);
		assertThat(quota.getLimit()).isEqualTo(100);
		assertThat(quota.getRemaining()).isEqualTo(80);
	}
}
