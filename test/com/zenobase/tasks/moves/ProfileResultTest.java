package com.zenobase.tasks.moves;

import static org.fest.assertions.Assertions.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.Test;

import com.zenobase.tasks.ResultTestSupport;

public class ProfileResultTest extends ResultTestSupport {

	@Test
	public void test() {
		ProfileResult result = new ProfileResult(readObject("ProfileResultTest.json"));
		DateTimeZone zone = DateTimeZone.forID("Europe/Helsinki");
		DateTime expected = LocalDateTime.parse("2012-12-11T00:00:00.000").toDateTime(zone);
		assertThat(result.getFirstDate()).isEqualTo(expected);
	}
}
