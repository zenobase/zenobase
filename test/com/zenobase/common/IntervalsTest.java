package com.zenobase.common;

import static org.hamcrest.Matchers.equalTo;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

public class IntervalsTest {

	@Test
	public void testForYear() {
		Interval year = Intervals.valueOf("2012T+0000");
		Assert.assertThat(year.getStart(), equalTo(new DateTime(2012, 1, 1, 0, 0, DateTimeZone.UTC)));
		Assert.assertThat(year.getEnd(), equalTo(new DateTime(2013, 1, 1, 0, 0, DateTimeZone.UTC)));
	}

	@Test
	public void testForMonth() {
		Interval month = Intervals.valueOf("2012-01T+0000");
		Assert.assertThat(month.getStart(), equalTo(new DateTime(2012, 1, 1, 0, 0, DateTimeZone.UTC)));
		Assert.assertThat(month.getEnd(), equalTo(new DateTime(2012, 2, 1, 0, 0, DateTimeZone.UTC)));
	}

	@Test
	public void testForDay() {
		Interval day = Intervals.valueOf("2012-01-01T+0000");
		Assert.assertThat(day.getStart(), equalTo(new DateTime(2012, 1, 1, 0, 0, DateTimeZone.UTC)));
		Assert.assertThat(day.getEnd(), equalTo(new DateTime(2012, 1, 2, 0, 0, DateTimeZone.UTC)));
	}

	@Test
	public void testForHour() {
		Interval day = Intervals.valueOf("2012-01-01T14+0000");
		Assert.assertThat(day.getStart(), equalTo(new DateTime(2012, 1, 1, 14, 0, DateTimeZone.UTC)));
		Assert.assertThat(day.getEnd(), equalTo(new DateTime(2012, 1, 1, 15, 0, DateTimeZone.UTC)));
	}

	@Test
	public void testForHourPST() {
		Interval day = Intervals.valueOf("2012-01-01T14-0800");
		Assert.assertThat(day.getStart(), equalTo(new DateTime(2012, 1, 1, 14, 0, DateTimeZone.forOffsetHours(-8))));
		Assert.assertThat(day.getEnd(), equalTo(new DateTime(2012, 1, 1, 15, 0, DateTimeZone.forOffsetHours(-8))));
	}

	@Test
	public void testForMinute() {
		Interval day = Intervals.valueOf("2012-01-01T14:59+0000");
		Assert.assertThat(day.getStart(), equalTo(new DateTime(2012, 1, 1, 14, 59, DateTimeZone.UTC)));
		Assert.assertThat(day.getEnd(), equalTo(new DateTime(2012, 1, 1, 15, 0, DateTimeZone.UTC)));
	}

	@Test
	public void testExpandMonth() {
		DateTime start = new DateTime(2012, 1, 1, 0, 0, DateTimeZone.UTC);
		DateTime end = new DateTime(2012, 2, 1, 0, 0, DateTimeZone.UTC);
		List<DateTime> days = Intervals.expand(start, end, "day");
		Assert.assertEquals(31, days.size());
		Assert.assertEquals(start, days.get(0));
	}
}
