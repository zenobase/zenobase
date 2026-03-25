package com.zenobase.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class QueryConstraintBuilderTest {

	@Test
	public void test() {
		QueryConstraint c = QueryConstraint.parse("foo:bar:baz");
		assertThat(c).isNotNull();
		assertThat(c.field()).as("field").isEqualTo("foo");
		assertThat(c.value()).as("value").isEqualTo("bar:baz");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testMissingColon() {
		QueryConstraint.parse("foobar");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testMissingField() {
		QueryConstraint.parse(":bar");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testMissingValue() {
		QueryConstraint.parse("foo:");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testEmpty() {
		QueryConstraint.parse("");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNull() {
		QueryConstraint.parse("");
	}
}
