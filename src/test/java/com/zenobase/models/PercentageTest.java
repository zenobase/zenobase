package com.zenobase.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.testing.EqualsTester;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

public class PercentageTest {

	@Test
	public void testEqualsHashCode() {
		Percentage r1 = valueOf("0");
		Percentage r2 = valueOf("50.5");
		new EqualsTester().addEqualityGroup(r1, Percentage.valueOf(r1.value())).addEqualityGroup(r2).testEquals();
	}

	@Test
	public void testCompareTo() {
		Percentage r1 = valueOf("50.0");
		Percentage r2 = valueOf("50.1");
		assertThat(r1.compareTo(r1)).isZero();
		assertThat(r1.compareTo(r2)).isNegative();
		assertThat(r2.compareTo(r1)).isPositive();
	}

	@Test
	public void testTooLowPercentage() {
		assertThatThrownBy(() -> valueOf("-1")).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testTooHighPercentage() {
		assertThatThrownBy(() -> valueOf("101")).isInstanceOf(IllegalArgumentException.class);
	}

	private static Percentage valueOf(String value) {
		return Percentage.valueOf(new BigDecimal(value));
	}
}
