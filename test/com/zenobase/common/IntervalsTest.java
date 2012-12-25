package com.zenobase.common;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Test;

public class IntervalsTest {

	@Test
	public void testForYear() {
		Interval year = Intervals.valueOf("2012TZ");
		assertThat(year.getStart()).isEqualTo(DateTime.parse("2012-01-01T00:00Z"));
		assertThat(year.getEnd()).isEqualTo(DateTime.parse("2013-01-01T00:00Z"));
	}

	@Test
	public void testForMonth() {
		Interval month = Intervals.valueOf("2012-01TZ");
		assertThat(month.getStart()).isEqualTo(DateTime.parse("2012-01-01T00:00Z"));
		assertThat(month.getEnd()).isEqualTo(DateTime.parse("2012-02-01T00:00Z"));
	}

	@Test
	public void testForDay() {
		Interval day = Intervals.valueOf("2012-01-01TZ");
		assertThat(day.getStart()).isEqualTo(DateTime.parse("2012-01-01T00:00Z"));
		assertThat(day.getEnd()).isEqualTo(DateTime.parse("2012-01-02T00:00Z"));
	}

	@Test
	public void testForHour() {
		Interval hour = Intervals.valueOf("2012-01-01T14Z");
		assertThat(hour.getStart()).isEqualTo(DateTime.parse("2012-01-01T14:00Z"));
		assertThat(hour.getEnd()).isEqualTo(DateTime.parse("2012-01-01T15:00Z"));
	}

	@Test
	public void testForHourPST() {
		Interval hour = Intervals.valueOf("2012-01-01T14-08:00");
		assertThat(hour.getStart()).isEqualTo(DateTime.parse("2012-01-01T14:00-08:00"));
		assertThat(hour.getEnd()).isEqualTo(DateTime.parse("2012-01-01T15:00-08:00"));
	}

	@Test
	public void testForMinute() {
		Interval minute = Intervals.valueOf("2012-01-01T14:59Z");
		assertThat(minute.getStart()).isEqualTo(DateTime.parse("2012-01-01T14:59Z"));
		assertThat(minute.getEnd()).isEqualTo(DateTime.parse("2012-01-01T15:00Z"));
	}

	@Test
	public void testForSecond() {
		Interval second = Intervals.valueOf("2012-01-01T14:00:05Z");
		assertThat(second.getStart()).isEqualTo(DateTime.parse("2012-01-01T14:00:05Z"));
		assertThat(second.getEnd()).isEqualTo(DateTime.parse("2012-01-01T14:00:06Z"));
	}

	@Test
	public void testExpandMonth() {
		DateTime start = DateTime.parse("2012-01-01T00:00Z");
		DateTime end = DateTime.parse("2012-02-01T00:00Z");
		List<DateTime> days = Intervals.expand(start, end, "day");
		assertThat(days.size()).isEqualTo(31);
		assertThat(days.get(0)).isEqualTo(start);
		assertThat(days.get(30)).isNotEqualTo(end);
	}

	@Test
	public void testExpandMillisecond() {
		DateTime start = DateTime.parse("2012-01-01T00:00:00.000Z");
		DateTime end = DateTime.parse("2012-01-01T00:00:00.010Z");
		List<DateTime> milliseconds = Intervals.expand(start, end, "millisecond");
		assertThat(milliseconds.size()).isEqualTo(10);
		assertThat(milliseconds.get(0)).isEqualTo(start);
	}

	@Test
	public void testToString() {
		DateTime time = DateTime.parse("2012-04-25T18:30Z");
		assertThat(Intervals.toString(time, "year")).as(time + " as year").isEqualTo("2012TZ");
		assertThat(Intervals.toString(time, "month")).as(time + " as month").isEqualTo("2012-04TZ");
		assertThat(Intervals.toString(time, "day")).as(time + " as day").isEqualTo("2012-04-25TZ");
		assertThat(Intervals.toString(time, "hour")).as(time + " as hour").isEqualTo("2012-04-25T18Z");
		assertThat(Intervals.toString(time, "minute")).as(time + " as minute").isEqualTo("2012-04-25T18:30Z");
		assertThat(Intervals.toString(time, "second")).as(time + " as second").isEqualTo("2012-04-25T18:30:00Z");
		assertThat(Intervals.toString(time, "millisecond")).as(time + " as millisecond").isEqualTo("2012-04-25T18:30:00.000Z");
	}

	@Test
	public void testToStringWithOffset() {
		DateTime time = DateTime.parse("2012-04-25T18:30-08:00");
		assertThat(Intervals.toString(time, "year")).as(time + " as year with offset").isEqualTo("2012T-08:00");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadFormat() {
		Intervals.valueOf("bla");
	}
}
