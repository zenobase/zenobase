package com.zenobase.models;

import com.google.common.base.Preconditions;
import com.google.common.collect.Range;
import java.math.BigDecimal;

public record Percentage(BigDecimal value) implements Comparable<Percentage> {
	private static final Range<BigDecimal> RANGE = Range.closed(BigDecimal.ZERO, BigDecimal.valueOf(100L));

	public Percentage {
		Preconditions.checkArgument(RANGE.contains(value), "Expected a value in %s but got: %s", RANGE, value);
	}

	public static Percentage valueOf(BigDecimal value) {
		return new Percentage(value);
	}

	public static Percentage valueOf(int value) {
		return valueOf(BigDecimal.valueOf(value));
	}

	@Override
	public int compareTo(Percentage that) {
		return value.compareTo(that.value);
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
