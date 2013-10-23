package com.zenobase.common;

import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.joda.time.LocalDateTime;
import org.junit.Test;

public class LocalIntervalsTest {

	@Test
	public void testForYear() {
		LocalInterval year = LocalIntervals.valueOf("2012");
		assertThat(year.getStart()).isEqualTo(LocalDateTime.parse("2012-01-01T00:00"));
		assertThat(year.getEnd()).isEqualTo(LocalDateTime.parse("2013-01-01T00:00"));
	}

	@Test
	public void testForMonth() {
		LocalInterval month = LocalIntervals.valueOf("2012-01");
		assertThat(month.getStart()).isEqualTo(LocalDateTime.parse("2012-01-01T00:00"));
		assertThat(month.getEnd()).isEqualTo(LocalDateTime.parse("2012-02-01T00:00"));
	}

	@Test
	public void testForDay() {
		LocalInterval day = LocalIntervals.valueOf("2012-01-01");
		assertThat(day.getStart()).isEqualTo(LocalDateTime.parse("2012-01-01T00:00"));
		assertThat(day.getEnd()).isEqualTo(LocalDateTime.parse("2012-01-02T00:00"));
	}

	@Test
	public void testForHour() {
		LocalInterval hour = LocalIntervals.valueOf("2012-01-01T14");
		assertThat(hour.getStart()).isEqualTo(LocalDateTime.parse("2012-01-01T14:00"));
		assertThat(hour.getEnd()).isEqualTo(LocalDateTime.parse("2012-01-01T15:00"));
	}

	@Test
	public void testForMinute() {
		LocalInterval minute = LocalIntervals.valueOf("2012-01-01T14:59");
		assertThat(minute.getStart()).isEqualTo(LocalDateTime.parse("2012-01-01T14:59"));
		assertThat(minute.getEnd()).isEqualTo(LocalDateTime.parse("2012-01-01T15:00"));
	}

	@Test
	public void testForSecond() {
		LocalInterval second = LocalIntervals.valueOf("2012-01-01T14:00:05");
		assertThat(second.getStart()).isEqualTo(LocalDateTime.parse("2012-01-01T14:00:05"));
		assertThat(second.getEnd()).isEqualTo(LocalDateTime.parse("2012-01-01T14:00:06"));
	}

	@Test
	public void testExpandMonth() {
		LocalDateTime start = LocalDateTime.parse("2012-01-01T00:00");
		LocalDateTime end = LocalDateTime.parse("2012-02-01T00:00");
		List<LocalDateTime> days = LocalIntervals.expand(start, end, "day");
		assertThat(days.size()).isEqualTo(31);
		assertThat(days.get(0)).isEqualTo(start);
		assertThat(days.get(30)).isNotEqualTo(end);
	}

	@Test
	public void testExpandMillisecond() {
		LocalDateTime start = LocalDateTime.parse("2012-01-01T00:00:00.000");
		LocalDateTime end = LocalDateTime.parse("2012-01-01T00:00:00.010");
		List<LocalDateTime> milliseconds = LocalIntervals.expand(start, end, "millisecond");
		assertThat(milliseconds.size()).isEqualTo(10);
		assertThat(milliseconds.get(0)).isEqualTo(start);
	}

	@Test
	public void testToString() {
		LocalDateTime time = LocalDateTime.parse("2012-04-25T18:30");
		assertThat(LocalIntervals.toString(time, "year")).as(time + " as year").isEqualTo("2012");
		assertThat(LocalIntervals.toString(time, "month")).as(time + " as month").isEqualTo("2012-04");
		assertThat(LocalIntervals.toString(time, "day")).as(time + " as day").isEqualTo("2012-04-25");
		assertThat(LocalIntervals.toString(time, "hour")).as(time + " as hour").isEqualTo("2012-04-25T18");
		assertThat(LocalIntervals.toString(time, "minute")).as(time + " as minute").isEqualTo("2012-04-25T18:30");
		assertThat(LocalIntervals.toString(time, "second")).as(time + " as second").isEqualTo("2012-04-25T18:30:00");
		assertThat(LocalIntervals.toString(time, "millisecond")).as(time + " as millisecond").isEqualTo("2012-04-25T18:30:00.000");
	}

	@Test
	public void testIllegalFormat() {
		assertThat(LocalIntervals.valueOf("bla")).isNull();
	}
}
