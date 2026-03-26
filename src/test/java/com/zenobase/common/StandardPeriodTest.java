package com.zenobase.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.testing.EqualsTester;
import org.joda.time.Period;
import org.junit.jupiter.api.Test;

public class StandardPeriodTest {

	@Test
	public void testParseToString() {
		roundtrip("+1y", Period.years(1));
		roundtrip("-1y", Period.years(-1));
		roundtrip("+12M", Period.months(12));
		roundtrip("+4w", Period.weeks(4));
		roundtrip("+7d", Period.days(7));
		roundtrip("+1h-2m+3s", Period.hours(1).withMinutes(-2).withSeconds(3));
	}

	private static void roundtrip(String s, Period period) {
		assertThat(StandardPeriod.valueOf(s).toPeriod()).isEqualTo(period);
		assertThat(StandardPeriod.valueOf(period).toString()).isEqualTo(s);
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

	@Test
	public void testCompareTo() {
		compare("+1y", "+1y", 0);
		compare("+1y", "+2y", -1);
		compare("+2y", "+1y", 1);
		compare("-1y", "+1y", -1);
		compare("+1y", "-1y", 1);
		compare("+1M", "+1y", -1);
		compare("+1y", "+1M", 1);
	}

	private static void compare(String a, String b, int expected) {
		assertThat(StandardPeriod.valueOf(a).compareTo(StandardPeriod.valueOf(b)))
				.isEqualTo(expected);
	}
}
