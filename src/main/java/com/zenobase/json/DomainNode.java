package com.zenobase.json;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.jspecify.annotations.Nullable;

public class DomainNode {

	public static final TokenField ID = new TokenField("@id", false);
	public static final LongField VERSION = new LongField("version", false);
	public static final LongField SEQ_NO = new LongField("seq_no", false);
	public static final LongField PRIMARY_TERM = new LongField("primary_term", false);

	private final ObjectNode node;

	public DomainNode(ObjectNode node) {
		this.node = Preconditions.checkNotNull(node);
	}

	public DomainNode() {
		node = Nodes.newObject();
	}

	public String getId() {
		return Objects.requireNonNull(getValue(ID));
	}

	public long getVersion() {
		return getValue(VERSION, 0L);
	}

	public void setVersion(long version) {
		setValue(VERSION, version);
	}

	protected <T> @Nullable T getValue(Field<T> field) {
		return field.getValue(node);
	}

	protected <T> T getValue(Field<T> field, T defaultValue) {
		return MoreObjects.firstNonNull(field.getValue(node), defaultValue);
	}

	protected <T> ImmutableList<T> getValues(Field<T> field) {
		return field.getValues(node);
	}

	protected <T> void setValue(Field<T> field, @Nullable T value) {
		field.setValue(node, value);
	}

	protected <T> void setValues(Field<T> field, Iterable<T> values) {
		field.setValues(node, values);
	}

	protected <T> void addValue(Field<T> field, @Nullable T value) {
		if (value != null) {
			field.addValue(node, value);
		}
	}

	protected <T> boolean contains(Field<T> field) {
		return node.has(field.getName());
	}

	protected <T> @Nullable T getValue(ObjectField parent, Field<T> child) {
		ObjectNode node = getValue(parent);
		return node != null ? child.getValue(node) : null;
	}

	protected <T> @Nullable Iterable<T> getValues(ObjectField parent, Field<T> child) {
		ObjectNode node = getValue(parent);
		return node != null ? child.getValues(node) : null;
	}

	public <T> void setValue(ObjectField parent, Field<T> child, @Nullable T value) {
		ObjectNode node = getValue(parent);
		if (node == null) {
			node = Nodes.newObject();
			setValue(parent, node);
		}
		child.setValue(node, value);
	}

	public <T> void setValues(ObjectField parent, Field<T> child, Iterable<T> values) {
		ObjectNode node = getValue(parent);
		if (node == null) {
			node = Nodes.newObject();
			setValue(parent, node);
		}
		child.setValues(node, values);
	}

	public ObjectNode toJson() {
		return node;
	}

	@Override
	public String toString() {
		return toJson().toString();
	}

	@Override
	public boolean equals(Object that) {
		return that instanceof DomainNode && equals((DomainNode) that);
	}

	private boolean equals(DomainNode that) {
		return node.equals(that.node);
	}

	@Override
	public int hashCode() {
		return node.hashCode();
	}

	protected <T extends DomainNode> T copy(Class<T> type) {
		return as(type, node.deepCopy());
	}

	public <T extends DomainNode> T as(Class<T> type) {
		return as(type, node);
	}

	private static <T extends DomainNode> T as(Class<T> type, ObjectNode node) {
		try {
			return type.getConstructor(ObjectNode.class).newInstance(node);
		} catch (InstantiationException
				| NoSuchMethodException
				| InvocationTargetException
				| IllegalAccessException e) {
			throw new AssertionError(e);
		}
	}
}
