package com.zenobase.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Range;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

public class OffsetDateTimeRangeParserTest {

	private final OffsetDateTimeRangeParser parser = new OffsetDateTimeRangeParser();

	@Test
	public void test() {
		DateTime lower = DateTime.parse("2000-01-01TZ");
		DateTime upper = DateTime.parse("2010-02-03T04:05:06.007-08:00");
		test("[2000TZ..2010-02-03T04:05:06.007-08:00)", Range.closedOpen(lower, upper));
	}

	@Test
	public void testClosedRangeWithYears() {
		DateTime lower = DateTime.parse("2000-01-01TZ");
		DateTime upper = DateTime.parse("2011-01-01TZ");
		test("[2000TZ..2010TZ]", Range.closedOpen(lower, upper));
	}

	@Test
	public void testOpenRangeWithYears() {
		DateTime lower = DateTime.parse("2001-01-01TZ");
		DateTime upper = DateTime.parse("2010-01-01TZ");
		test("(2000TZ..2010TZ)", Range.closedOpen(lower, upper));
	}

	private void test(String value, Range<DateTime> expected) {
		assertThat(parser.parse(value)).isEqualTo(expected);
	}
}
