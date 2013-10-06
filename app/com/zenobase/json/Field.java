package com.zenobase.json;

import java.lang.reflect.Type;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import com.zenobase.search.ConstraintBuilder;

public abstract class Field<T> {

	private final String name;
	private final Type type;
	private final String schemaType;
	private final Multimap<String, ConstraintBuilder> constraintBuilders = ArrayListMultimap.create();

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
		if (fieldNode != null) {
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
		else if (fieldNode != null) {
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

	public void setValues(ObjectNode node, Iterable<T> values) {
		ArrayNode arrayNode = node.putArray(name);
		addValues(arrayNode, values);
	}

	private void addValues(ArrayNode node, Iterable<T> values) {
		for (T value : values) {
			Preconditions.checkNotNull(value, "Can't add null value");
			node.add(toJson(value));
		}
	}

	public abstract JsonNode toJson(T value);

	public void createSchema(ObjectNode schema) {
		configureSchema(schema.putObject(getName()));
	}

	public void configureSchema(ObjectNode schema) {
		schema.put("type", schemaType);
	}

	protected static void configureSchema(ObjectNode properties, Field<?> field) {
		field.configureSchema(properties.putObject(field.getName()));
	}

	public Multimap<String, ConstraintBuilder> getConstraintBuilders() {
		return constraintBuilders;
	}

	protected void addConstraintBuilder(ConstraintBuilder constraint) {
		addConstraintBuilder(name, constraint);
	}

	protected void addConstraintBuilders(Field<?> nested) {
		for (Map.Entry<String, ConstraintBuilder> entry : nested.getConstraintBuilders().entries()) {
			addConstraintBuilder(name + "." + entry.getKey(), wrap(entry.getValue()));
		}
	}

	protected void addConstraintBuilder(String name, ConstraintBuilder constraint) {
		constraintBuilders.put(name, constraint);
	}

	protected ConstraintBuilder wrap(ConstraintBuilder builder) {
		return builder;
	}

	public void prePersist(ObjectNode node) {

	}

	@Override
	public String toString() {
		return name;
	}
}
