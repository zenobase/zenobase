package models;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.common.base.Objects;

import schema.Field;
import schema.LongField;

import com.google.common.collect.ImmutableList;
import common.Nodes;

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
		field.setValues(node, field.getName(), values);
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
