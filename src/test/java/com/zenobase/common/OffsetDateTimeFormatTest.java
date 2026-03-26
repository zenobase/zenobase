package com.zenobase.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class OffsetDateTimeFormatTest {

	@ParameterizedTest
	@CsvSource({
		"2012TZ, 2012-01-01T00:00:00.000Z",
		"2012-02TZ, 2012-02-01T00:00:00.000Z",
		"2013-W01TZ, 2012-12-31T00:00:00.000Z",
		"2012-02-03TZ, 2012-02-03T00:00:00.000Z",
		"2012-02-03T04Z, 2012-02-03T04:00:00.000Z",
		"2012-02-03T04:05Z, 2012-02-03T04:05:00.000Z",
		"2012-02-03T04:05:06Z, 2012-02-03T04:05:06.000Z",
		"2012-02-03T04:05:06.007Z, 2012-02-03T04:05:06.007Z",
		"2012T-08:00, 2012-01-01T00:00:00.000-08:00",
	})
	public void testParse(String value, String expected) {
		DateTime actual = OffsetDateTimeFormat.parse(value);
		assertThat(actual).isEqualTo(DateTime.parse(expected));
	}

	@Test
	public void testParseBadInput() {
		assertThatThrownBy(() -> OffsetDateTimeFormat.parse("xxx")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testParseBadCasing() {
		assertThatThrownBy(() -> OffsetDateTimeFormat.parse("2020-12-15t19:22:19.933z"))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
