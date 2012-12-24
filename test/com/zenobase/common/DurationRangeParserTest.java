package com.zenobase.common;

import static org.fest.assertions.Assertions.assertThat;

import org.joda.time.Duration;
import org.junit.Test;
import com.google.common.collect.Range;
import com.google.common.collect.Ranges;

public class DurationRangeParserTest {

	private final DurationRangeParser parser = new DurationRangeParser();

	@Test
	public void testClosedOpenRange() {
		Duration lower = Duration.standardHours(1L);
		Duration upper = Duration.standardDays(1L);
		testRange("[1h..1d)", Ranges.closedOpen(lower, upper));
	}

	@Test
	public void testOpenClosedRange() {
		Duration lower = Duration.millis(500L);
		Duration upper = Duration.standardSeconds(1L);
		testRange("(500..1s]", Ranges.openClosed(lower, upper));
	}

	private void testRange(String value, Range<Duration> expected) {
		assertThat(parser.parse(value)).isEqualTo(expected);
	}
}
