package com.zenobase.common;

import static org.fest.assertions.Assertions.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import com.google.common.collect.Range;

public class DateTimeRangeParserTest {

	private final DateTime now = DateTime.now(DateTimeZone.UTC);
	private final DateTimeRangeParser parser = new DateTimeRangeParser() {
		@Override
		protected DateTime now() {
			return now;
		}
	};

	@Test
	public void testYears() {
		DateTime lower = DateTime.parse("2000-01-01TZ");
		DateTime upper = DateTime.parse("2010-02-03T04:05:06.007-08:00");
		testRange("[2000TZ..2010-02-03T04:05:06.007-08:00)", Range.closedOpen(lower, upper));
	}

	@Test
	public void testRelative() {
		testRange("[-1y..+1y]", Range.closed(now.minusYears(1), now.plusYears(1)));
		testRange("[-1y 2M..+3y 4h]", Range.closed(now.minusYears(1).minusMonths(2), now.plusYears(3).plusHours(4)));
	}

	private void testRange(String value, Range<DateTime> expected) {
		assertThat(parser.parse(value)).isEqualTo(expected);
	}
}
