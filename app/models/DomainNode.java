package models;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.common.collect.Iterables;

import schema.TokenField;
import schema.Field;

import com.google.common.collect.ImmutableList;
import common.Nodes;

public class DomainNode {

	public static final TokenField ID = new TokenField("@id");

	private final ObjectNode object;

	public DomainNode(ObjectNode object) {
		this.object = object;
	}

	public DomainNode(String id) {
		object = Nodes.newObject();
		ID.setValue(object, id);
	}

	public String getId() {
		return Iterables.getOnlyElement(ID.getValues(object));
	}

	protected <T> T getValue(Field<T> field) {
		return Iterables.getOnlyElement(getValues(field), null);
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

	public ObjectNode toJson() {
		return object;
	}
}
