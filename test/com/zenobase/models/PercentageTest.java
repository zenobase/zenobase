package com.zenobase.models;

import static org.fest.assertions.Assertions.assertThat;

import java.math.BigDecimal;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

public class PercentageTest {

	@Test
	public void testEqualsHashCode() {
		Percentage r1 = valueOf("0");
		Percentage r2 = valueOf("50.5");
		new EqualsTester()
			.addEqualityGroup(r1, Percentage.valueOf(r1.getValue()))
			.addEqualityGroup(r2).testEquals();
	}

	@Test
	public void testCompareTo() {
		Percentage r1 = valueOf("50.0");
		Percentage r2 = valueOf("50.1");
		assertThat(r1.compareTo(r1)).isZero();
		assertThat(r1.compareTo(r2)).isNegative();
		assertThat(r2.compareTo(r1)).isPositive();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTooLowPercentage() {
		valueOf("-1");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTooHighPercentage() {
		valueOf("101");
	}

	private static Percentage valueOf(String value) {
		return Percentage.valueOf(new BigDecimal(value));
	}
}
