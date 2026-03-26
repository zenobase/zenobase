package com.zenobase.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.joda.time.Duration;
import org.junit.jupiter.api.Test;

public class DurationFormatTest {

	@Test
	public void testParseDays() {
		assertThat(DurationFormat.parse("1d")).isEqualTo(Duration.standardDays(1L));
	}

	@Test
	public void testParseHoursMinutesSeconds() {
		assertThat(DurationFormat.parse("1h 1min 1s")).isEqualTo(Duration.standardSeconds(3600L + 60L + 1L));
	}

	@Test
	public void testParseMillis() {
		assertThat(DurationFormat.parse("10000")).isEqualTo(Duration.standardSeconds(10L));
	}

	@Test
	public void testParseBadFormat() {
		assertThatThrownBy(() -> DurationFormat.parse("1 h")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testParseBadUnit() {
		assertThatThrownBy(() -> DurationFormat.parse("1parsec")).isInstanceOf(IllegalArgumentException.class);
	}
}
