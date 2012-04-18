package com.zenobase.models;

import com.google.common.base.Preconditions;

public class Rating implements Comparable<Rating> {

	private final int value;

	private Rating(int value) {
		Preconditions.checkArgument(value >= 0 && value <= 100,
			"Expected a rating between 0 and 100: %s", value);
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
