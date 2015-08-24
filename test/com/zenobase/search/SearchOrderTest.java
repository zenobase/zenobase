package com.zenobase.search;

import com.google.common.testing.EqualsTester;
import org.fest.assertions.Assertions;
import org.junit.Test;

import com.zenobase.json.Schema;
import com.zenobase.json.SchemaBuilder;
import com.zenobase.json.TokenField;
import com.zenobase.services.SearchOrder;

public class SearchOrderTest {

	private static final Schema SCHEMA = new SchemaBuilder("test")
		.add(new TokenField("foo"))
		.add(new TokenField("bar")).build();

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
		Assertions.assertThat(SearchOrder.valueOf(s, SCHEMA).toString()).isEqualTo(s);
	}

	@Test
	public void testEqualsHashCode() {
		new EqualsTester()
			.addEqualityGroup(SearchOrder.valueOf("foo", SCHEMA), SearchOrder.valueOf("foo", SCHEMA))
			.addEqualityGroup(SearchOrder.valueOf("-foo", SCHEMA))
			.addEqualityGroup(SearchOrder.valueOf("-bar", SCHEMA))
			.testEquals();
	}
}
