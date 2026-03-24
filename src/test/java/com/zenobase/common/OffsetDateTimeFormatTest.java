package com.zenobase.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.joda.time.DateTime;
import org.junit.Test;

public class OffsetDateTimeFormatTest {

	@Test
	public void testParse() {
		test("2012TZ", "2012-01-01T00:00:00.000Z");
		test("2012-02TZ", "2012-02-01T00:00:00.000Z");
		test("2013-W01TZ", "2012-12-31T00:00:00.000Z");
		test("2012-02-03TZ", "2012-02-03T00:00:00.000Z");
		test("2012-02-03T04Z", "2012-02-03T04:00:00.000Z");
		test("2012-02-03T04:05Z", "2012-02-03T04:05:00.000Z");
		test("2012-02-03T04:05:06Z", "2012-02-03T04:05:06.000Z");
		test("2012-02-03T04:05:06.007Z", "2012-02-03T04:05:06.007Z");
	}

	@Test
	public void testParseWithOffset() {
		test("2012T-08:00", "2012-01-01T00:00:00.000-08:00");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParseBadInput() {
		test("xxx", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testParseBadCasing() {
		OffsetDateTimeFormat.parse("2020-12-15t19:22:19.933z");
	}

	private static void test(String value, String expected) {
		DateTime actual = OffsetDateTimeFormat.parse(value);
		assertThat(actual).isEqualTo(DateTime.parse(expected));
	}
}
