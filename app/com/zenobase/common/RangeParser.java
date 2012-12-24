package com.zenobase.common;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import com.google.common.collect.Ranges;

public abstract class RangeParser<T extends Comparable<T>> {

	private final static String TO = "..";

	public Range<T> parse(String value) {
		int p = value.indexOf(TO);
		try {
			return p != -1 ?
				toRange(
					value.charAt(0),
					value.substring(1, p),
					value.substring(p + TO.length(), value.length() - 1),
					value.charAt(value.length() - 1)) :
				null;
		} catch (Exception e) {
			throw new IllegalArgumentException("Can't parse range: " + value);
		}
	}

	private Range<T> toRange(char lowerType, String lower, String upper, char upperType) {
		return toRange(
			getBoundType(lowerType),
			getOptionalValue(lower),
			getOptionalValue(upper),
			getBoundType(upperType));
	}

	private Range<T> toRange(BoundType lowerType, T lower, T upper, BoundType upperType) {
		if (lower == null) {
			if (upper == null) {
				return Ranges.all();
			} else {
				return Ranges.upTo(upper, upperType);
			}
		} else if (upper == null) {
			return Ranges.downTo(lower, lowerType);
		} else {
			return Ranges.range(lower, lowerType, upper, upperType);
		}
	}

	private BoundType getBoundType(char symbol) {
		switch (symbol) {
			case '[':
			case ']':
				return BoundType.CLOSED;
			case '(':
			case ')':
				return BoundType.OPEN;
			default:
				return null;
		}
	}

	private T getOptionalValue(String s) {
		return "*".equals(s) ? null : getValue(s);
	}

	protected abstract T getValue(String s);
}
