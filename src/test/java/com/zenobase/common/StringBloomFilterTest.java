package com.zenobase.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class StringBloomFilterTest {

	@Test
	public void test() {
		StringBloomFilter filter = new StringBloomFilter(3);
		assertThat(filter.mightContain("foo")).isFalse();
		filter.put("foo");
		assertThat(filter.mightContain("foo")).isTrue();
	}
}
