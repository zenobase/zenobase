package models;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.common.collect.Iterables;

import schema.Field;
import schema.TokenType;

import com.google.common.collect.ImmutableList;
import common.Nodes;

public class DomainNode {

	public static final Field<Token> ID = Field.of("@id", new TokenType());

	private final ObjectNode object;

	public DomainNode(ObjectNode object) {
		this.object = object;
	}

	public DomainNode(String id) {
		object = Nodes.newObject();
		ID.getType().set(object, ID.getName(), Token.valueOf(id));
	}

	public String getId() {
		return Iterables.getOnlyElement(ID.getType().get(object, ID.getName())).toString();
	}

	protected <T> T getValue(Field<T> field) {
		return Iterables.getOnlyElement(getValues(field));
	}

	protected <T> ImmutableList<T> getValues(Field<T> field) {
		return field.getType().get(object, field.getName());
	}

	protected <T> void setValue(Field<T> field, T value) {
		field.getType().set(object, field.getName(), value);
	}

	protected <T> void setValues(Field<T> field, Iterable<T> values) {
		field.getType().set(object, field.getName(), values);
	}

	protected <T> void addValue(Field<T> field, T value) {
		field.getType().add(object, field.getName(), value);
	}

	protected <T> void addValues(Field<T> field, Iterable<T> values) {
		field.getType().add(object, field.getName(), values);
	}

	public ObjectNode toJson() {
		return object;
	}
}
