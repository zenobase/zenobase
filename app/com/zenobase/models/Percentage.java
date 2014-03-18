package com.zenobase.models;

import java.math.BigDecimal;

import com.google.common.base.Preconditions;
import com.google.common.collect.Range;

public class Percentage implements Comparable<Percentage> {

	private static final Range<BigDecimal> RANGE = Range.closed(BigDecimal.ZERO, BigDecimal.valueOf(100L));

	private final BigDecimal value;

	private Percentage(BigDecimal value) {
		Preconditions.checkArgument(RANGE.contains(value),
			"Expected a value in %s but got: %s", RANGE, value);
		this.value = value;
	}

	public static Percentage valueOf(BigDecimal value) {
		return new Percentage(value);
	}

	public static Percentage valueOf(int value) {
		return valueOf(BigDecimal.valueOf(value));
	}

	public BigDecimal getValue() {
		return value;
	}

	@Override
	public int compareTo(Percentage that) {
		return value.compareTo(that.value);
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof Percentage && equals((Percentage) that);
	}

	private boolean equals(Percentage that) {
		return value.equals(that.value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
