package com.zenobase.common;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.junit.Test;

public class IntervalsTest {

	@Test
	public void testForYear() {
		Interval year = Intervals.valueOf("2012T+0000");
		assertThat(year.getStart()).isEqualTo(new DateTime(2012, 1, 1, 0, 0, DateTimeZone.UTC));
		assertThat(year.getEnd()).isEqualTo(new DateTime(2013, 1, 1, 0, 0, DateTimeZone.UTC));
	}

	@Test
	public void testForMonth() {
		Interval month = Intervals.valueOf("2012-01T+0000");
		assertThat(month.getStart()).isEqualTo(new DateTime(2012, 1, 1, 0, 0, DateTimeZone.UTC));
		assertThat(month.getEnd()).isEqualTo(new DateTime(2012, 2, 1, 0, 0, DateTimeZone.UTC));
	}

	@Test
	public void testForDay() {
		Interval day = Intervals.valueOf("2012-01-01T+0000");
		assertThat(day.getStart()).isEqualTo(new DateTime(2012, 1, 1, 0, 0, DateTimeZone.UTC));
		assertThat(day.getEnd()).isEqualTo(new DateTime(2012, 1, 2, 0, 0, DateTimeZone.UTC));
	}

	@Test
	public void testForHour() {
		Interval day = Intervals.valueOf("2012-01-01T14+0000");
		assertThat(day.getStart()).isEqualTo(new DateTime(2012, 1, 1, 14, 0, DateTimeZone.UTC));
		assertThat(day.getEnd()).isEqualTo(new DateTime(2012, 1, 1, 15, 0, DateTimeZone.UTC));
	}

	@Test
	public void testForHourPST() {
		Interval day = Intervals.valueOf("2012-01-01T14-0800");
		assertThat(day.getStart()).isEqualTo(new DateTime(2012, 1, 1, 14, 0, DateTimeZone.forOffsetHours(-8)));
		assertThat(day.getEnd()).isEqualTo(new DateTime(2012, 1, 1, 15, 0, DateTimeZone.forOffsetHours(-8)));
	}

	@Test
	public void testForMinute() {
		Interval day = Intervals.valueOf("2012-01-01T14:59+0000");
		assertThat(day.getStart()).isEqualTo(new DateTime(2012, 1, 1, 14, 59, DateTimeZone.UTC));
		assertThat(day.getEnd()).isEqualTo(new DateTime(2012, 1, 1, 15, 0, DateTimeZone.UTC));
	}

	@Test
	public void testExpandMonth() {
		DateTime start = new DateTime(2012, 1, 1, 0, 0, DateTimeZone.UTC);
		DateTime end = new DateTime(2012, 2, 1, 0, 0, DateTimeZone.UTC);
		List<DateTime> days = Intervals.expand(start, end, "day");
		assertThat(days.size()).isEqualTo(31);
		assertThat(days.get(0)).isEqualTo(start);
	}

	@Test
	public void testToString() {
		DateTime time = new DateTime(2012, 4, 25, 18, 30, DateTimeZone.UTC);
		assertThat(Intervals.toString(time, "year")).as(time + " as year").isEqualTo("2012T+0000");
		assertThat(Intervals.toString(time, "month")).as(time + " as month").isEqualTo("2012-04T+0000");
		assertThat(Intervals.toString(time, "day")).as(time + " as day").isEqualTo("2012-04-25T+0000");
		assertThat(Intervals.toString(time, "hour")).as(time + " as hour").isEqualTo("2012-04-25T18+0000");
		assertThat(Intervals.toString(time, "minute")).as(time + " as minute").isEqualTo("2012-04-25T18:30+0000");
		assertThat(Intervals.toString(time, "second")).as(time + " as second").isEqualTo("2012-04-25T18:30:00+0000");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadFormat() {
		Intervals.valueOf("bla");
	}
}
