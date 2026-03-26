package com.zenobase.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class QueryConstraintBuilderTest {

	@Test
	public void test() {
		QueryConstraint c = QueryConstraint.parse("foo:bar:baz");
		assertThat(c).isNotNull();
		assertThat(c.field()).as("field").isEqualTo("foo");
		assertThat(c.value()).as("value").isEqualTo("bar:baz");
	}

	@ParameterizedTest
	@ValueSource(strings = {"foobar", ":bar", "foo:", ""})
	public void testInvalidInput(String input) {
		assertThatThrownBy(() -> QueryConstraint.parse(input)).isInstanceOf(IllegalArgumentException.class);
	}
}
