package com.zenobase.common;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;

public abstract class RangeParser<T extends Comparable<T>> {

	private static final String TO = "..";

	public Range<T> parse(String value) {
		int p = value.indexOf(TO);
		if (p == -1 || value.length() < 6) {
			return null;
		}
		BoundType lowerBound = getBoundType(value.charAt(0));
		BoundType upperBound = getBoundType(value.charAt(value.length() - 1));
		if (lowerBound == null || upperBound == null) {
			return null;
		}
		T lower = getOptionalValue(value.substring(1, p));
		T upper = getOptionalValue(value.substring(p + TO.length(), value.length() - 1));
		if (lower == null && upper == null) {
			return null;
		}
		return toRange(lowerBound, lower, upper, upperBound);
	}

	private Range<T> toRange(BoundType lowerType, T lower, T upper, BoundType upperType) {
		if (lower == null) {
			return Range.upTo(upper, upperType);
		} else if (upper == null) {
			return Range.downTo(lower, lowerType);
		} else {
			return Range.range(lower, lowerType, upper, upperType);
		}
	}

	private BoundType getBoundType(char symbol) {
		return switch (symbol) {
			case '[', ']' -> BoundType.CLOSED;
			case '(', ')' -> BoundType.OPEN;
			default -> null;
		};
	}

	private T getOptionalValue(String s) {
		return "*".equals(s) ? null : getValue(s);
	}

	protected abstract T getValue(String s);
}
