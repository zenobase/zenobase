package com.zenobase.schema;

import java.lang.reflect.Type;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public abstract class Field<T> {

	private final String name;
	private final Type type;
	private final String schemaType;

	protected Field(String name, Type type, String schemaType) {
		this.name = name;
		this.type = type;
		this.schemaType = schemaType;
	}

	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}

	public T getValue(ObjectNode node) {
		JsonNode fieldNode = node.get(name);
		if (fieldNode != null && fieldNode.isArray() && fieldNode.size() > 0) {
			return getValue(Iterables.getOnlyElement(((ArrayNode) fieldNode)));
		}
		if (fieldNode != null && !fieldNode.isMissingNode() && !fieldNode.isNull()) {
			return getValue(fieldNode);
		}
		return null;
	}

	public ImmutableList<T> getValues(ObjectNode node) {
		ImmutableList.Builder<T> values = ImmutableList.builder();
		JsonNode fieldNode = node.get(name);
		if (fieldNode != null && fieldNode.isArray()) {
			for (JsonNode element : ((ArrayNode) fieldNode)) {
				values.add(getValue(element));
			}
		}
		else if (fieldNode != null && !fieldNode.isMissingNode() && !fieldNode.isNull()) {
			values.add(getValue(fieldNode));
		}
		return values.build();
	}

	protected ImmutableList<JsonNode> getNodes(ObjectNode node) {
		ImmutableList.Builder<JsonNode> values = ImmutableList.builder();
		JsonNode fieldNode = node.get(name);
		if (fieldNode != null && fieldNode.isArray()) {
			for (JsonNode element : ((ArrayNode) fieldNode)) {
				values.add(element);
			}
		}
		else if (fieldNode != null && !fieldNode.isMissingNode() && !fieldNode.isNull()) {
			values.add(fieldNode);
		}
		return values.build();
	}

	protected abstract T getValue(JsonNode node);

	public void addValue(ObjectNode node, T value) {
		Preconditions.checkNotNull(value, "Can't add null value");
		JsonNode fieldNode = node.get(name);
		if (fieldNode == null) {
			ArrayNode arrayNode = node.putArray(name);
			arrayNode.add(toJson(value));
		}
		else if (fieldNode.isArray()) {
			ArrayNode arrayNode = ((ArrayNode) fieldNode);
			arrayNode.add(toJson(value));
		}
		else {
			ArrayNode arrayNode = node.putArray(name);
			arrayNode.add(fieldNode);
			arrayNode.add(toJson(value));
		}
	}

	public void addValues(ObjectNode node, Iterable<T> values) {
		JsonNode fieldNode = node.get(name);
		if (fieldNode == null) {
			ArrayNode arrayNode = node.putArray(name);
			addValues(arrayNode, values);
		}
		else if (fieldNode.isArray()) {
			ArrayNode arrayNode = ((ArrayNode) fieldNode);
			addValues(arrayNode, values);
		}
		else {
			ArrayNode arrayNode = node.putArray(name);
			arrayNode.add(fieldNode);
			addValues(arrayNode, values);
		}
	}

	public void setValue(ObjectNode node, T value) {
		if (value != null) {
			node.put(name, toJson(value));
		}
		else {
			node.remove(name);
		}
	}

	public void setValues(ObjectNode node, String fieldName, Iterable<T> values) {
		ArrayNode arrayNode = node.putArray(fieldName);
		addValues(arrayNode, values);
	}

	private void addValues(ArrayNode node, Iterable<T> values) {
		for (T value : values) {
			Preconditions.checkNotNull(value, "Can't add null value");
			node.add(toJson(value));
		}
	}

	protected abstract JsonNode toJson(T value);

	public void configureSchema(ObjectNode schema) {
		schema.put("type", schemaType);
	}

	public void prePersist(ObjectNode node) {

	}

	public void postLoad(ObjectNode node) {

	}
}
