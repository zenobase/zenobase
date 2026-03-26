package com.zenobase.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class QueryConstraintBuilderTest {

	@Test
	public void test() {
		QueryConstraint c = QueryConstraint.parse("foo:bar:baz");
		assertThat(c).isNotNull();
		assertThat(c.field()).as("field").isEqualTo("foo");
		assertThat(c.value()).as("value").isEqualTo("bar:baz");
	}

	@Test
	public void testMissingColon() {
		assertThatThrownBy(() -> QueryConstraint.parse("foobar")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testMissingField() {
		assertThatThrownBy(() -> QueryConstraint.parse(":bar")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testMissingValue() {
		assertThatThrownBy(() -> QueryConstraint.parse("foo:")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testEmpty() {
		assertThatThrownBy(() -> QueryConstraint.parse("")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testNull() {
		assertThatThrownBy(() -> QueryConstraint.parse("")).isInstanceOf(IllegalArgumentException.class);
	}
}
