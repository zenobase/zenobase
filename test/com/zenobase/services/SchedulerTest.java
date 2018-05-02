package com.zenobase.services;

import static org.fest.assertions.Assertions.assertThat;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.junit.Test;

public class SchedulerTest {

	@Test
	public void testNextExecution() {
		test("2014-12-13T17:50:00Z", new LocalTime(0, 0), Period.minutes(5), Duration.ZERO);
		test("2014-12-13T17:50:01Z", new LocalTime(0, 0), Period.minutes(5), Duration.standardSeconds(299));
		test("2014-12-13T17:50:00Z", new LocalTime(18, 0), Period.hours(12), Duration.standardMinutes(10));
		test("2014-12-13T18:01:00Z", new LocalTime(18, 0), Period.hours(3), Duration.standardMinutes(179));
		test("2014-12-13T18:01:00Z", new LocalTime(18, 0), Period.hours(6), Duration.standardMinutes(359));
	}

	private static void test(String now, LocalTime begin, Period repeat, Duration expected) {
		assertThat(Scheduler.nextExecution(DateTime.parse(now), begin, repeat)).isEqualTo(expected);
	}
}
