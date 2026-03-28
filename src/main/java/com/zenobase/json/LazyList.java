package com.zenobase.json;

import java.util.AbstractList;
import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.common.PartialList;

public abstract class LazyList<T extends DomainNode> extends AbstractList<T> implements PartialList<T> {

	private final PartialList<ObjectNode> nodes;

	public LazyList(PartialList<ObjectNode> nodes) {
		this.nodes = nodes;
	}

	@Override
	public T get(int index) {
		return toObject(nodes.get(index));
	}

	protected abstract T toObject(ObjectNode node);

	@Override
	public int size() {
		return nodes.size();
	}

	@Override
	public long getTotal() {
		return nodes.getTotal();
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof LazyList<?> l && nodes.equals(l.nodes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(nodes);
	}
}
