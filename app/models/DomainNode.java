package models;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.common.base.Objects;

import schema.Field;

import com.google.common.collect.ImmutableList;
import common.Nodes;

public class DomainNode {

	private final ObjectNode object;

	public DomainNode(ObjectNode object) {
		this.object = object;
	}

	public DomainNode() {
		object = Nodes.newObject();
	}

	protected <T> T getValue(Field<T> field) {
		return field.getValue(object);
	}

	protected <T> T getValue(Field<T> field, T defaultValue) {
		return Objects.firstNonNull(field.getValue(object), defaultValue);
	}

	protected <T> ImmutableList<T> getValues(Field<T> field) {
		return field.getValues(object);
	}

	protected <T> void setValue(Field<T> field, T value) {
		field.setValue(object, value);
	}

	protected <T> void setValues(Field<T> field, Iterable<T> values) {
		field.setValues(object, field.getName(), values);
	}

	protected <T> void addValue(Field<T> field, T value) {
		field.addValue(object, value);
	}

	protected <T> void addValues(Field<T> field, Iterable<T> values) {
		field.addValues(object, values);
	}

	protected <T> boolean contains(Field<T> field) {
		return object.has(field.getName());
	}

	public ObjectNode toJson() {
		return object;
	}
}
