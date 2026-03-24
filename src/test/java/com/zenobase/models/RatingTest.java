package com.zenobase.models;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

public class RatingTest {

	@Test
	public void testEqualsHashCode() {
		Rating r1 = Rating.valueOf(0);
		Rating r2 = Rating.valueOf(50);
		new EqualsTester()
			.addEqualityGroup(r1, Rating.valueOf(r1.getValue()))
			.addEqualityGroup(r2).testEquals();
	}

	@Test
	public void testCompareTo() {
		Rating r1 = Rating.valueOf(0);
		Rating r2 = Rating.valueOf(50);
		assertThat(r1.compareTo(r1)).isZero();
		assertThat(r1.compareTo(r2)).isNegative();
		assertThat(r2.compareTo(r1)).isPositive();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTooLowRating() {
		Rating.valueOf(-1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTooHighRating() {
		Rating.valueOf(101);
	}
}
