package com.zenobase.common;

import com.google.common.collect.ImmutableList;

import com.zenobase.json.IntegerField;

public class PartialList<T> {

	protected static final IntegerField TOTAL = new IntegerField("total");

	private final ImmutableList<T> elements;
	private final long size;

	public PartialList(Iterable<T> elements, long size) {
		this.elements = ImmutableList.copyOf(elements);
		this.size = size;
	}

	public ImmutableList<T> getElements() {
		return elements;
	}

	public long size() {
		return size;
	}
}
