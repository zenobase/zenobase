package com.zenobase.common;

import static org.fest.assertions.Assertions.assertThat;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;

public class CustomDateTimeFormatTest {

	@Test
	public void test() {
		test("2012TZ", "2012-01-01T00:00:00.000Z");
		test("2012-02TZ", "2012-02-01T00:00:00.000Z");
		test("2012-02-03TZ", "2012-02-03T00:00:00.000Z");
		test("2012-02-03T04Z", "2012-02-03T04:00:00.000Z");
		test("2012-02-03T04:05Z", "2012-02-03T04:05:00.000Z");
		test("2012-02-03T04:05:06Z", "2012-02-03T04:05:06.000Z");
		test("2012-02-03T04:05:06.007Z", "2012-02-03T04:05:06.007Z");
	}

	@Test
	public void testWithOffset() {
		test("2012T-08:00", "2012-01-01T00:00:00.000-08:00");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadFormat() {
		test("xxx", null);
	}

	private static void test(String value, String expected) {
		DateTime actual = CustomDateTimeFormat.format().parseDateTime(value);
		assertThat(actual).isEqualTo(DateTime.parse(expected));
	}

	@Test
	public void testYearToString() {
		test(CustomDateTimeFormat.inclYear(), "2012-02-03T04:05:06.007Z", "2012TZ");
	}

	private static void test(DateTimeFormatter formatter, String timestamp, String expected) {
		assertThat(formatter.print(DateTime.parse(timestamp))).isEqualTo(expected);
	}
}
