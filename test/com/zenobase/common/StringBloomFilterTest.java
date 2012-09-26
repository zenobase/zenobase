package com.zenobase.common;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

public class StringBloomFilterTest {

	@Test
	public void test() {
		StringBloomFilter filter = new StringBloomFilter(3);
		assertThat(filter.mightContain("foo")).isFalse();
		filter.put("foo");
		assertThat(filter.mightContain("foo")).isTrue();
	}
}
