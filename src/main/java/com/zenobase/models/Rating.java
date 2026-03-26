package com.zenobase.models;

import com.google.common.base.Preconditions;

public record Rating(int value) implements Comparable<Rating> {

	public static final int MIN_VALUE = 0;
	public static final int MAX_VALUE = 100;

	public Rating {
		Preconditions.checkArgument(
				value >= MIN_VALUE && value <= MAX_VALUE,
				"Expected a rating between %s and %s: %s",
				MIN_VALUE,
				MAX_VALUE,
				value);
	}

	public static Rating valueOf(int value) {
		return new Rating(value);
	}

	@Override
	public int compareTo(Rating that) {
		return value - that.value;
	}

	@Override
	public String toString() {
		return Integer.toString(value);
	}
}
