package com.zenobase.common;

import static org.fest.assertions.Assertions.assertThat;

import org.joda.time.DateTime;
import org.junit.Test;
import com.google.common.collect.Range;

public class DateTimeRangeParserTest {

	private final DateTimeRangeParser parser = new DateTimeRangeParser();

	@Test
	public void test() {
		DateTime lower = DateTime.parse("2000-01-01TZ");
		DateTime upper = DateTime.parse("2010-02-03T04:05:06.007-08:00");
		test("[2000TZ..2010-02-03T04:05:06.007-08:00)", Range.closedOpen(lower, upper));
	}

	private void test(String value, Range<DateTime> expected) {
		assertThat(parser.parse(value)).isEqualTo(expected);
	}
}
