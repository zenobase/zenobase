package com.zenobase.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.Test;

public class DateTimeZonesTest {

	@Test
	public void test() {
		DateTimeZone timezone = DateTimeZone.forID("Europe/Berlin");
		assertThatIsEqualTo(timezone, "2012-03-25T01:04:00.000", "2012-03-25T01:04:00.000+01:00");
		assertThatIsEqualTo(timezone, "2012-03-25T02:04:00.000", "2012-03-25T03:04:00.000+02:00");
		assertThatIsEqualTo(timezone, "2012-03-25T03:04:00.000", "2012-03-25T03:04:00.000+02:00");
	}

	private static void assertThatIsEqualTo(DateTimeZone timezone, String local, String expected) {
		assertThat(DateTimeZones.toDateTime(LocalDateTime.parse(local), timezone)
						.toString())
				.isEqualTo(DateTime.parse(expected).toString());
	}

	@Test
	public void testMyanmar() {
		DateTimeZone.forID("Asia/Yangon"); // replaces Asia/Rangoon in tzdb-2016g
	}
}
