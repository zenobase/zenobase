package com.zenobase.common;

import java.util.AbstractList;
import java.util.Collections;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class DefaultPartialList<T> extends AbstractList<T> implements PartialList<T> {

	private final ImmutableList<T> elements;
	private final long total;

	public static <T> PartialList<T> of(Iterable<T> elements, long total) {
		return new DefaultPartialList<T>(elements, total);
	}

	public static <T> PartialList<T> of() {
		return new DefaultPartialList<T>(Collections.<T>emptyList(), 0L);
	}

	public static <T> PartialList<T> of(T... elements) {
		return new DefaultPartialList<T>(ImmutableList.copyOf(elements), elements.length);
	}

	protected DefaultPartialList(Iterable<T> elements, long total) {
		this.elements = ImmutableList.copyOf(elements);
		this.total = total;
	}

	@Override
	public T get(int index) {
		return elements.get(index);
	}

	@Override
	public int size() {
		return elements.size();
	}

	@Override
	public long getTotal() {
		return total;
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof PartialList &&
			equals((PartialList<?>) that);
	}

	private boolean equals(PartialList<?> that) {
		return total == that.getTotal() &&
			Iterables.elementsEqual(elements, that);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(elements, total);
	}
}
