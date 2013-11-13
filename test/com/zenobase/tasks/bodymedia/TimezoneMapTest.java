package com.zenobase.tasks.bodymedia;

import static org.fest.assertions.Assertions.assertThat;

import org.fest.assertions.ObjectAssert;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.junit.Test;

public class TimezoneMapTest {

	private static final DateTimeZone LOS_ANGELES = DateTimeZone.forID("America/Los_Angeles");
	private static final DateTimeZone AMSTERDAM = DateTimeZone.forID("Europe/Amsterdam");

	private final TimezoneMap map = new TimezoneMap();

	@Test
	public void test() {

		add("2012-12-30T19:36:14-08:00", "2013-05-09T23:30:00-0700", LOS_ANGELES);
		add("2013-05-10T08:30:00+0200", "2013-05-13T22:00:00+0200", AMSTERDAM);
		add("2013-05-13T13:00:00-0700", null, LOS_ANGELES);

		assertThat(map.getBegin()).isEqualTo(LocalDate.parse("2012-12-30"));

		assertBegin("2012-12-30", "2012-12-30T00:00:00-08:00");
		assertBegin("2013-05-09", "2013-05-09T00:00:00-07:00");
		assertBegin("2013-05-10", "2013-05-10T08:30:00+02:00");
		assertBegin("2013-05-13", "2013-05-13T00:00:00+02:00");
		assertBegin("2013-05-14", "2013-05-14T00:00:00-07:00");

		assertZone("2012-12-30T00:00:00Z", null);
		assertZone("2012-12-31T03:36:13Z", LOS_ANGELES);
		assertZone("2012-12-31T03:36:14Z", LOS_ANGELES);
		assertZone("2013-05-10T06:29:59Z", LOS_ANGELES);
		assertZone("2013-05-10T06:30:00Z", AMSTERDAM);
		assertZone("2013-05-13T20:00:00Z", LOS_ANGELES);
		assertZone("2013-06-01T19:00:00Z", LOS_ANGELES);
	}

	private void add(String from, String to, DateTimeZone timezone) {
		map.add(DateTime.parse(from), to != null ? DateTime.parse(to) : null, timezone);
	}

	private void assertBegin(String date, String time) {
		assertThat(map.getBegin(LocalDate.parse(date)).toString()).isEqualTo(DateTime.parse(time).toString());
	}

	private ObjectAssert assertZone(String time, DateTimeZone expected) {
		DateTime rezoned = map.rezone(DateTime.parse(time));
		return assertThat(rezoned != null ? rezoned.getZone() : null).isEqualTo(expected);
	}
}
