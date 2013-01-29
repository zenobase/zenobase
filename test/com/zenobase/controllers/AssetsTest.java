package com.zenobase.controllers;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

public class AssetsTest {

	@Test
	public void testStripCacheBuster() {
		assertThat(Assets.stripCacheBuster("test-a1234567.txt")).isEqualTo("test.txt");
		assertThat(Assets.stripCacheBuster("test-1.0.txt")).isEqualTo("test-1.0.txt");
	}
}
