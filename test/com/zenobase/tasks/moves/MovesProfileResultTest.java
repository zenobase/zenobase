package com.zenobase.tasks.moves;

import static org.fest.assertions.Assertions.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.Test;

import com.zenobase.tasks.ResultTestSupport;

public class MovesProfileResultTest extends ResultTestSupport {

	@Test
	public void test() {
		MovesProfileResult result = new MovesProfileResult(readObject("MovesProfileResultTest.json"));
		DateTimeZone zone = DateTimeZone.forID("Europe/Helsinki");
		DateTime expected = LocalDateTime.parse("2012-12-11T00:00:00.000").toDateTime(zone);
		assertThat(result.getFirstDate()).isEqualTo(expected);
	}
}
