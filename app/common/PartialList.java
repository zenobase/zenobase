package common;

import com.google.common.collect.ImmutableList;

public class PartialList<T> {

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
