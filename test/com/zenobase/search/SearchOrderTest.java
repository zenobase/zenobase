package com.zenobase.search;

import org.fest.assertions.Assertions;
import org.junit.Test;
import com.google.common.testing.EqualsTester;

import com.zenobase.services.SearchOrder;

public class SearchOrderTest {

	@Test
	public void test() {
		assertThatToStringEqualsValueOf("foo");
		assertThatToStringEqualsValueOf("-foo");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyString() {
		assertThatToStringEqualsValueOf("");
	}

	private static void assertThatToStringEqualsValueOf(String s) {
		Assertions.assertThat(SearchOrder.valueOf(s).toString()).isEqualTo(s);
	}

	@Test
	public void testEqualsHashCode() {
		new EqualsTester()
			.addEqualityGroup(SearchOrder.valueOf("foo"), SearchOrder.valueOf("foo"))
			.addEqualityGroup(SearchOrder.valueOf("-foo"))
			.addEqualityGroup(SearchOrder.valueOf("-bar"))
			.testEquals();
	}
}
