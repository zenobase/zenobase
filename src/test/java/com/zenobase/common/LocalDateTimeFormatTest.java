package com.zenobase.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class LocalDateTimeFormatTest {

	@ParameterizedTest
	@CsvSource(
		{
			"2012, 2012-01-01T00:00:00.000",
			"2012-02, 2012-02-01T00:00:00.000",
			"2012-02-03, 2012-02-03T00:00:00.000",
			"2013-W01, 2012-12-31T00:00:00.000",
			"2012-02-03T04, 2012-02-03T04:00:00.000",
			"2012-02-03T04:05, 2012-02-03T04:05:00.000",
			"2012-02-03T04:05:06, 2012-02-03T04:05:06.000",
			"2012-02-03T04:05:06.007, 2012-02-03T04:05:06.007",
		}
	)
	public void testParse(String value, String expected) {
		LocalDateTime actual = LocalDateTimeFormat.parse(value);
		assertThat(actual).isEqualTo(LocalDateTime.parse(expected));
	}

	@Test
	public void testParseBadInput() {
		assertThatThrownBy(() -> LocalDateTimeFormat.parse("xxx")).isInstanceOf(IllegalArgumentException.class);
	}
}
