package com.zenobase.common;

import java.util.AbstractList;
import java.util.Collections;
import java.util.Objects;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class DefaultPartialList<T> extends AbstractList<T> implements PartialList<T> {

	private final ImmutableList<T> elements;
	private final long total;

	public static <T> PartialList<T> of(Iterable<T> elements, long total) {
		return new DefaultPartialList<>(elements, total);
	}

	public static <T> PartialList<T> of() {
		return new DefaultPartialList<>(Collections.emptyList(), 0L);
	}

	public static <T> PartialList<T> of(T element) {
		return new DefaultPartialList<>(ImmutableList.of(element), 1);
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
		return that instanceof PartialList<?> list
				&& total == list.getTotal()
				&& Iterables.elementsEqual(elements, list);
	}

	@Override
	public int hashCode() {
		return Objects.hash(elements, total);
	}
}
