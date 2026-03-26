package com.zenobase.services;

import com.google.common.testing.EqualsTester;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.zenobase.json.Field;
import com.zenobase.json.TokenField;

public class QueryTest {

	private static final Field<?> FIELD = new TokenField("tag");

	@Test
	public void testEmpty() {
		assertThatQueryBuildsTo(new QuerySupport(), "{\"match_all\":{}}");
	}

	@Test
	public void testEqualTo() {
		assertThatQueryBuildsTo(new QuerySupport().equalTo(FIELD, "foo"), "{\"term\":{\"tag\":{\"value\":\"foo\"}}}");
	}

	@Test
	public void testEqualToNull() {
		Assertions.assertThat(new QuerySupport().equalTo(FIELD, null)).isEqualTo(new QuerySupport().isNull(FIELD));
	}

	@Test
	public void testIsNull() {
		assertThatQueryBuildsTo(
				new QuerySupport().isNull(FIELD), "{\"bool\":{\"must_not\":[{\"exists\":{\"field\":\"tag\"}}]}}");
	}

	@Test
	public void testNotNull() {
		assertThatQueryBuildsTo(new QuerySupport().notNull(FIELD), "{\"exists\":{\"field\":\"tag\"}}");
	}

	@Test
	public void testLessThan() {
		assertThatQueryBuildsTo(new QuerySupport().lessThan(FIELD, 10), "{\"range\":{\"tag\":{\"lt\":10}}}");
	}

	@Test
	public void testBoolean() {
		assertThatQueryBuildsTo(
				new QuerySupport().equalTo(FIELD, "foo").equalTo(FIELD, "bar"),
				"{\"bool\":{\"must\":[{\"term\":{\"tag\":{\"value\":\"foo\"}}},{\"term\":{\"tag\":{\"value\":\"bar\"}}}]}}");
	}

	private static void assertThatQueryBuildsTo(QuerySupport query, String expected) {
		Assertions.assertThat(normalize(query.build().toJsonString())).isEqualTo(normalize(expected));
	}

	private static String normalize(String s) {
		return s.replaceAll("\\s+", "");
	}

	@Test
	public void testEqualsHashCode() {
		QuerySupport q1 = new QuerySupport().equalTo(FIELD, "foo");
		QuerySupport q2 = new QuerySupport().equalTo(FIELD, "foo");
		QuerySupport q3 = new QuerySupport().equalTo(FIELD, "bar");
		QuerySupport q4 = new QuerySupport();
		new EqualsTester()
				.addEqualityGroup(q1, q2)
				.addEqualityGroup(q3)
				.addEqualityGroup(q4)
				.testEquals();
	}
}
