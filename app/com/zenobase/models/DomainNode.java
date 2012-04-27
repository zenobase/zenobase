package com.zenobase.models;

import org.codehaus.jackson.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

import com.zenobase.common.Nodes;
import com.zenobase.json.Field;
import com.zenobase.json.LongField;

public class DomainNode {

	public static final LongField VERSION = new LongField("version", false);

	private final ObjectNode node;

	public DomainNode(ObjectNode node) {
		this.node = node;
	}

	public DomainNode() {
		node = Nodes.newObject();
	}

	public long getVersion() {
		return getValue(VERSION);
	}

	public void setVersion(long version) {
		setValue(VERSION, version);
	}

	protected <T> T getValue(Field<T> field) {
		return field.getValue(node);
	}

	protected <T> T getValue(Field<T> field, T defaultValue) {
		return Objects.firstNonNull(field.getValue(node), defaultValue);
	}

	protected <T> ImmutableList<T> getValues(Field<T> field) {
		return field.getValues(node);
	}

	protected <T> void setValue(Field<T> field, T value) {
		field.setValue(node, value);
	}

	protected <T> void setValues(Field<T> field, Iterable<T> values) {
		field.setValues(node, values);
	}

	protected <T> void addValue(Field<T> field, T value) {
		field.addValue(node, value);
	}

	protected <T> void addValues(Field<T> field, Iterable<T> values) {
		field.addValues(node, values);
	}

	protected <T> boolean contains(Field<T> field) {
		return node.has(field.getName());
	}

	public ObjectNode toJson() {
		return node;
	}
}
