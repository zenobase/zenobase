package com.zenobase.common;

import static org.fest.assertions.Assertions.assertThat;

import com.google.common.collect.Range;
import org.joda.time.LocalDateTime;
import org.joda.time.ReadablePartial;
import org.junit.Test;

public class LocalDateTimeRangeParserTest {

	private final LocalDateTimeRangeParser parser = new LocalDateTimeRangeParser();

	@Test
	public void test() {
		LocalDateTime lower = LocalDateTime.parse("2000-01-01");
		LocalDateTime upper = LocalDateTime.parse("2010-02-03T04:05:06.007");
		test("[2000..2010-02-03T04:05:06.007)", Range.closedOpen(lower, upper));
	}

	@Test
	public void testClosedRangeWithYears() {
		LocalDateTime lower = LocalDateTime.parse("2000-01-01");
		LocalDateTime upper = LocalDateTime.parse("2011-01-01");
		test("[2000..2010]", Range.closedOpen(lower, upper));
	}

	@Test
	public void testOpenRangeWithYears() {
		LocalDateTime lower = LocalDateTime.parse("2001-01-01");
		LocalDateTime upper = LocalDateTime.parse("2010-01-01");
		test("(2000..2010)", Range.closedOpen(lower, upper));
	}

	private void test(String value, Range<? extends ReadablePartial> expected) {
		assertThat(parser.parse(value)).isEqualTo(expected);
	}
}
