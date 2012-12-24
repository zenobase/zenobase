package com.zenobase.common;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import com.google.common.collect.Ranges;

public abstract class RangeParser<T extends Comparable<T>> {

	private final static String TO = "..";

	public Range<T> parse(String value) {
		try {
			int p = value.indexOf(TO);
			BoundType lowerType = getBoundType(value.charAt(0));
			T lower = getOptionalValue(value.substring(1, p));
			T upper = getOptionalValue(value.substring(p + TO.length(), value.length() - 1));
			BoundType upperType = getBoundType(value.charAt(value.length() - 1));
			return toRange(lowerType, lower, upper, upperType);
		} catch (Exception e) {
			throw new IllegalArgumentException("Can't parse range: " + value);
		}
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
