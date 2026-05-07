package com.zenobase.queries;

import static org.assertj.core.api.Assertions.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class UserQueryTest {

	@Test
	public void testIsVerifiedTrue() {
		assertThatBuildsTo(new UserQuery().isVerified(true), "{\"term\":{\"verified\":{\"value\":true}}}");
	}

	@Test
	public void testIsVerifiedFalseMatchesMissing() {
		assertThatBuildsTo(
			new UserQuery().isVerified(false),
			"{\"bool\":{\"must_not\":[{\"term\":{\"verified\":{\"value\":true}}}]}}"
		);
	}

	@Test
	public void testIsSuspendedFalseMatchesMissing() {
		assertThatBuildsTo(
			new UserQuery().isSuspended(false),
			"{\"bool\":{\"must_not\":[{\"term\":{\"suspended\":{\"value\":true}}}]}}"
		);
	}

	@Test
	public void testCreatedBefore() {
		var t = new DateTime(2026, 1, 1, 0, 0, DateTimeZone.UTC);
		assertThatBuildsTo(
			new UserQuery().createdBefore(t),
			"{\"range\":{\"created\":{\"lt\":\"2026-01-01T00:00:00.000Z\"}}}"
		);
	}

	private static void assertThatBuildsTo(UserQuery query, String expected) {
		assertThat(normalize(query.build().toJsonString())).isEqualTo(normalize(expected));
	}

	private static String normalize(String s) {
		return s.replaceAll("\\s+", "");
	}
}
