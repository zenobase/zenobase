package com.zenobase.tasks.beeminder;

import static org.fest.assertions.Assertions.assertThat;

import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.zenobase.tasks.ResultTestSupport;

public class UserResultTest extends ResultTestSupport {

	@Test
	public void test() {
		UserResult result = new UserResult(readObject("UserResultTest.json"));
		assertThat(result.getTimezone()).isEqualTo(DateTimeZone.forID("America/Los_Angeles"));
		assertThat(result.hasGoal("bar")).isTrue();
		assertThat(result.hasGoal("baz")).isFalse();
	}
}
