package com.zenobase.services;

import com.google.common.testing.EqualsTester;
import org.fest.assertions.Assertions;
import org.junit.Test;

import com.zenobase.json.Field;
import com.zenobase.json.TokenField;

public class QueryTest {

	private static final Field<?> FIELD = new TokenField("tag");

	@Test
	public void testEmpty() {
		assertThatQueryBuildsTo(new QuerySupport(),
			"{ " +
			"  \"match_all\" : { \"boost\" : 1.0 } " +
			"}");
	}

	@Test
	public void testEqualTo() {
		assertThatQueryBuildsTo(new QuerySupport().equalTo(FIELD, "foo"),
			"{ " +
			"  \"term\" : { " +
			"    \"tag\" : { \"value\" : \"foo\", \"boost\" : 1.0 } " +
			"  }" +
			"}");
	}

	@Test
	public void testEqualToNull() {
		Assertions.assertThat(new QuerySupport().equalTo(FIELD, null))
			.isEqualTo(new QuerySupport().isNull(FIELD));
	}

	@Test
	public void testIsNull() {
		assertThatQueryBuildsTo(new QuerySupport().isNull(FIELD),
			"{ " +
			"  \"bool\" : { " +
			"    \"must_not\" : [{ " +
			"      \"exists\" : { \"field\" : \"tag\", \"boost\" : 1.0 } " +
			"    }], " +
			"    \"adjust_pure_negative\" : true, " +
			"    \"boost\" : 1.0 " +
			"  } " +
			"}");
	}

	@Test
	public void testNotNull() {
		assertThatQueryBuildsTo(new QuerySupport().notNull(FIELD),
			"{ " +
			"  \"exists\" : { " +
			"    \"field\" : \"tag\", " +
			"    \"boost\" : 1.0 " +
			"  } " +
			"}");
	}

	@Test
	public void testLessThan() {
		assertThatQueryBuildsTo(new QuerySupport().lessThan(FIELD, 10),
			"{ " +
			"  \"range\" : { " +
			"    \"tag\" : { " +
			"      \"from\" : null, " +
			"      \"to\" : 10, " +
			"      \"include_lower\" : true, " +
			"      \"include_upper\" : false, " +
			"      \"boost\" : 1.0 " +
			"    }" +
			"  }" +
			"}");
	}

	@Test
	public void testBoolean() {
		assertThatQueryBuildsTo(new QuerySupport().equalTo(FIELD, "foo").equalTo(FIELD, "bar"),
			"{ " +
			"  \"bool\" : { " +
			"    \"must\" : [{ " +
			"      \"term\" : { " +
			"        \"tag\" : { \"value\" : \"foo\", \"boost\" : 1.0 } " +
			"      } " +
			"    }, { " +
			"      \"term\" : { " +
			"        \"tag\" : { \"value\" : \"bar\", \"boost\" : 1.0 } " +
			"      } " +
			"    }], " +
			"    \"adjust_pure_negative\" : true, " +
			"    \"boost\" : 1.0 " +
			"  } " +
			"}");
	}

	private static void assertThatQueryBuildsTo(QuerySupport query, String expected) {
		Assertions.assertThat(normalize(query.build().toString())).isEqualTo(normalize(expected));
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
