package com.zenobase.common;

import static org.fest.assertions.Assertions.assertThat;

import org.joda.time.Period;
import org.junit.Test;

public class PeriodFormatTest {

	@Test
	public void testParseYears() {
		assertThat(PeriodFormat.parse("1y")).isEqualTo(Period.years(1));
	}

	@Test
	public void testParseMonths() {
		assertThat(PeriodFormat.parse("1M")).isEqualTo(Period.months(1));
	}

	@Test
	public void testParseWeeks() {
		assertThat(PeriodFormat.parse("1w")).isEqualTo(Period.weeks(1));
	}

	@Test
	public void testParseDays() {
		assertThat(PeriodFormat.parse("1d")).isEqualTo(Period.days(1));
	}

	@Test
	public void testParseHoursMinutesSeconds() {
		assertThat(PeriodFormat.parse("1h 2min 3s")).isEqualTo(Period.hours(1).withMinutes(2).withSeconds(3));
	}

	@Test
	public void testParseMillis() {
		assertThat(PeriodFormat.parse("10000")).isEqualTo(Period.millis(10000));
		assertThat(PeriodFormat.parse("10000ms")).isEqualTo(Period.millis(10000));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParseBadFormat() {
		PeriodFormat.parse("1 h");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParseBadUnit() {
		PeriodFormat.parse("1parsec");
	}
}
