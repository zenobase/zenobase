package com.zenobase.tasks.bodymedia;

import static org.fest.assertions.Assertions.assertThat;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import com.google.common.collect.RangeMap;

import com.zenobase.tasks.ResultTestSupport;

public class BodyMediaTimezonesResultTest extends ResultTestSupport {

	@Test
	public void test() {

		BodyMediaTimezonesResult result = new BodyMediaTimezonesResult(readObject("BodyMediaTimezonesResultTest.json"));
		RangeMap<LocalDateTime, DateTimeZone> timezones = result.getTimezones();
		assertThat(timezones.asMapOfRanges()).hasSize(3);
		assertThat(timezones.get(LocalDateTime.parse("2012-12-30T19:36:13"))).isNull();
		assertThat(timezones.get(LocalDateTime.parse("2012-12-30T19:36:14"))).isEqualTo(DateTimeZone.forID("US/Pacific"));
		assertThat(timezones.get(LocalDateTime.parse("2013-05-10T08:29:59"))).isNull(); // unfortunate, but true
		assertThat(timezones.get(LocalDateTime.parse("2013-05-10T08:30:00"))).isEqualTo(DateTimeZone.forID("Europe/Amsterdam"));
		assertThat(timezones.get(LocalDateTime.parse("2013-06-01T12:00:00"))).isEqualTo(DateTimeZone.forID("US/Pacific"));
	}
}
