package com.zenobase.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.testing.EqualsTester;
import org.joda.time.Period;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class StandardPeriodTest {

	@ParameterizedTest
	@CsvSource(
		{
			"+1y, 0, 0, 0, 1, 0, 0, 0",
			"-1y, 0, 0, 0, -1, 0, 0, 0",
			"+12M, 0, 0, 12, 0, 0, 0, 0",
			"+4w, 0, 4, 0, 0, 0, 0, 0",
			"+7d, 7, 0, 0, 0, 0, 0, 0",
		}
	)
	public void testParseToString(
		String s,
		int days,
		int weeks,
		int months,
		int years,
		int hours,
		int minutes,
		int seconds
	) {
		Period period = Period.years(years)
			.withMonths(months)
			.withWeeks(weeks)
			.withDays(days)
			.withHours(hours)
			.withMinutes(minutes)
			.withSeconds(seconds);
		assertThat(StandardPeriod.valueOf(s).toPeriod()).isEqualTo(period);
		assertThat(StandardPeriod.valueOf(period).toString()).isEqualTo(s);
	}

	@Test
	public void testParseToStringCompound() {
		Period period = Period.hours(1).withMinutes(-2).withSeconds(3);
		assertThat(StandardPeriod.valueOf("+1h-2m+3s").toPeriod()).isEqualTo(period);
		assertThat(StandardPeriod.valueOf(period).toString()).isEqualTo("+1h-2m+3s");
	}

	@Test
	public void testParseIllegalFormat() {
		assertThatThrownBy(() -> StandardPeriod.valueOf("+1y-2")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testParseIllegalUnit() {
		assertThatThrownBy(() -> StandardPeriod.valueOf("+1x")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testEqualsHashCode() {
		new EqualsTester()
			.addEqualityGroup(StandardPeriod.valueOf(Period.weeks(1)), StandardPeriod.valueOf(Period.weeks(1)))
			.addEqualityGroup(StandardPeriod.valueOf(Period.days(7)), StandardPeriod.valueOf(Period.days(7)))
			.addEqualityGroup(StandardPeriod.valueOf(Period.days(7).withHours(12)))
			.testEquals();
	}

	@ParameterizedTest
	@CsvSource(
		{ "+1y, +1y, 0", "+1y, +2y, -1", "+2y, +1y, 1", "-1y, +1y, -1", "+1y, -1y, 1", "+1M, +1y, -1", "+1y, +1M, 1" }
	)
	public void testCompareTo(String a, String b, int expected) {
		assertThat(StandardPeriod.valueOf(a).compareTo(StandardPeriod.valueOf(b))).isEqualTo(expected);
	}
}
