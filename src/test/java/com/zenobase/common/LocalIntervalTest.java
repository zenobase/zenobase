package com.zenobase.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.testing.EqualsTester;
import org.joda.time.LocalDateTime;
import org.junit.Test;

public class LocalIntervalTest {

	private final LocalDateTime start = LocalDateTime.parse("2013-01-01T00:00");
	private final LocalDateTime end = LocalDateTime.parse("2013-01-02T00:00");

	@Test
	public void test() {
		LocalInterval interval = new LocalInterval(start, end);
		assertThat(interval.getStart()).isEqualTo(start);
		assertThat(interval.getEnd()).isEqualTo(end);
		assertThat(interval.contains(start)).isTrue();
		assertThat(interval.contains(end)).isFalse();
		assertThat(interval.toDurationMillis()).isEqualTo(86400000L);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIllegalInterval() {
		new LocalInterval(end, start);
	}

	@Test
	public void testEqualsHashCode() {
		new EqualsTester()
				.addEqualityGroup(start, LocalDateTime.parse("2013-01-01T00:00"))
				.addEqualityGroup(end)
				.testEquals();
	}
}
