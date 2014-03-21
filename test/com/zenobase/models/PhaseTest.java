package com.zenobase.models;

import static org.fest.assertions.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.Test;
import com.google.common.testing.EqualsTester;

public class PhaseTest {

	@Test
	public void testEqualsHashCode() {
		Phase p1 = valueOf("0.0");
		Phase p2 = valueOf("0.5");
		new EqualsTester()
			.addEqualityGroup(p1, Phase.valueOf(p1.getValue()))
			.addEqualityGroup(p2).testEquals();
	}

	@Test
	public void testCompareTo() {
		Phase p1 = valueOf("0.50");
		Phase p2 = valueOf("0.75");
		assertThat(p1.compareTo(p1)).isZero();
		assertThat(p1.compareTo(p2)).isNegative();
		assertThat(p2.compareTo(p1)).isPositive();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTooLow() {
		valueOf("-1.0");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTooHigh() {
		valueOf("1.0");
	}

	private static Phase valueOf(String value) {
		return Phase.valueOf(new BigDecimal(value));
	}
}
