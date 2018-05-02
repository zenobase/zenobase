package com.zenobase.models;

import com.google.common.base.Preconditions;

public class Rating implements Comparable<Rating> {

	public static final int MIN_VALUE = 0;
	public static final int MAX_VALUE = 100;

	private final int value;

	private Rating(int value) {
		Preconditions.checkArgument(value >= MIN_VALUE && value <= MAX_VALUE,
			"Expected a rating between %s and %s: %s", MIN_VALUE, MAX_VALUE, value);
		this.value = value;
	}

	public static Rating valueOf(int value) {
		return new Rating(value);
	}

	public int getValue() {
		return value;
	}

	@Override
	public int compareTo(Rating that) {
		return value - that.value;
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof Rating && equals((Rating) that);
	}

	private boolean equals(Rating that) {
		return value == that.value;
	}

	@Override
	public int hashCode() {
		return value;
	}

	@Override
	public String toString() {
		return Integer.toString(value);
	}
}
