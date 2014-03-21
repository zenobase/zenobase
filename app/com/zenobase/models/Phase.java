package com.zenobase.models;

import java.math.BigDecimal;

import com.google.common.base.Preconditions;
import com.google.common.collect.Range;

public class Phase implements Comparable<Phase> {

	private static final Range<BigDecimal> RANGE = Range.closedOpen(BigDecimal.ZERO, BigDecimal.ONE);

	private final BigDecimal value;

	private Phase(BigDecimal value) {
		Preconditions.checkArgument(RANGE.contains(value),
			"Expected a value in %s but got: %s", RANGE, value);
		this.value = value;
	}

	public static Phase valueOf(BigDecimal value) {
		return new Phase(value);
	}

	public static Phase valueOf(double value) {
		return valueOf(BigDecimal.valueOf(value));
	}

	public BigDecimal getValue() {
		return value;
	}

	@Override
	public int compareTo(Phase that) {
		return value.compareTo(that.value);
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof Phase && equals((Phase) that);
	}

	private boolean equals(Phase that) {
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
