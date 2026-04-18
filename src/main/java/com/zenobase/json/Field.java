package com.zenobase.json;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import org.jspecify.annotations.Nullable;

import com.zenobase.search.constraints.ConstraintBuilder;

public abstract class Field<T> {

	private final String path;
	private final String name;
	private final Type type;
	private final String schemaType;
	private final ListMultimap<String, ConstraintBuilder> constraintBuilders = ArrayListMultimap.create();

	protected Field(String name, Type type, String schemaType) {
		this(name, name, type, schemaType);
	}

	protected Field(String path, String name, Type type, String schemaType) {
		this.path = path;
		this.name = name;
		this.type = type;
		this.schemaType = schemaType;
	}

	public String getPath() {
		return path;
	}

	public String getPathForSorting() {
		return getPath();
	}

	public final String getName() {
		return name;
	}

	public final Type getType() {
		return type;
	}

	public final String getSchemaType() {
		return schemaType;
	}

	public final @Nullable T getValue(ObjectNode node) {
		JsonNode fieldNode = node.get(name);
		if (fieldNode != null && fieldNode.isArray() && !fieldNode.isEmpty()) {
			return getValue(Objects.requireNonNull(Iterables.getOnlyElement(fieldNode)));
		}
		if (fieldNode != null) {
			return getValue(fieldNode);
		}
		return null;
	}

	public final ImmutableList<T> getValues(ObjectNode node) {
		ImmutableList.Builder<T> values = ImmutableList.builder();
		JsonNode fieldNode = node.get(name);
		if (fieldNode != null && fieldNode.isArray()) {
			for (JsonNode element : fieldNode) {
				T value = getValue(element);
				if (value != null) {
					values.add(value);
				}
			}
		} else if (fieldNode != null) {
			T value = getValue(fieldNode);
			if (value != null) {
				values.add(value);
			}
		}
		return values.build();
	}

	protected final ImmutableList<JsonNode> getNodes(ObjectNode node) {
		ImmutableList.Builder<JsonNode> values = ImmutableList.builder();
		JsonNode fieldNode = node.get(name);
		if (fieldNode != null && fieldNode.isArray()) {
			for (JsonNode element : fieldNode) {
				values.add(element);
			}
		} else if (fieldNode != null && !fieldNode.isMissingNode() && !fieldNode.isNull()) {
			values.add(fieldNode);
		}
		return values.build();
	}

	protected abstract @Nullable T getValue(JsonNode node);

	public final void addValue(ObjectNode node, T value) {
		Preconditions.checkNotNull(value, "Can't add null value");
		JsonNode fieldNode = node.get(name);
		if (fieldNode == null) {
			ArrayNode arrayNode = node.putArray(name);
			arrayNode.add(toJson(value));
		} else if (fieldNode.isArray()) {
			ArrayNode arrayNode = ((ArrayNode) fieldNode);
			arrayNode.add(toJson(value));
		} else {
			ArrayNode arrayNode = node.putArray(name);
			arrayNode.add(fieldNode);
			arrayNode.add(toJson(value));
		}
	}

	public final void addValues(ObjectNode node, Iterable<T> values) {
		JsonNode fieldNode = node.get(name);
		if (fieldNode == null) {
			ArrayNode arrayNode = node.putArray(name);
			addValues(arrayNode, values);
		} else if (fieldNode.isArray()) {
			ArrayNode arrayNode = ((ArrayNode) fieldNode);
			addValues(arrayNode, values);
		} else {
			ArrayNode arrayNode = node.putArray(name);
			arrayNode.add(fieldNode);
			addValues(arrayNode, values);
		}
	}

	public final void setValue(ObjectNode node, @Nullable T value) {
		if (value != null) {
			node.set(name, toJson(value));
		} else {
			node.remove(name);
		}
	}

	public final void setValues(ObjectNode node, Iterable<T> values) {
		ArrayNode arrayNode = node.putArray(name);
		addValues(arrayNode, values);
	}

	private void addValues(ArrayNode node, Iterable<T> values) {
		for (T value : values) {
			Preconditions.checkNotNull(value, "Can't add null value");
			node.add(toJson(value));
		}
	}

	public abstract JsonNode toJson(@Nullable T value);

	public void createSchema(ObjectNode schema) {
		configureSchema(schema.putObject(getName()));
	}

	public void configureSchema(ObjectNode schema) {
		schema.put("type", schemaType);
	}

	public JsonSchema toJsonSchema() {
		return switch (schemaType) {
			case "keyword", "text" -> JsonSchema.string();
			case "date" -> JsonSchema.string("date-time");
			case "byte", "integer", "long" -> JsonSchema.integer();
			case "float", "double" -> JsonSchema.number();
			default -> new JsonSchema(schemaType, null, null, null, null, null);
		};
	}

	protected static void configureSchema(ObjectNode properties, Field<?> field) {
		field.configureSchema(properties.putObject(field.getName()));
	}

	public final ListMultimap<String, ConstraintBuilder> getConstraintBuilders() {
		return constraintBuilders;
	}

	protected final void addConstraintBuilder(String path, ConstraintBuilder constraint) {
		constraintBuilders.put(path, constraint);
	}

	protected final void addAll(ListMultimap<String, ConstraintBuilder> constraints) {
		for (Map.Entry<String, ConstraintBuilder> entry : constraints.entries()) {
			addConstraintBuilder(entry.getKey(), entry.getValue());
		}
	}

	protected final void copyConstraintBuilders(Field<?> target) {
		for (Map.Entry<String, ConstraintBuilder> entry : getConstraintBuilders().entries()) {
			target.addConstraintBuilder(entry.getKey(), entry.getValue());
		}
	}

	public void prePersist(ObjectNode node) {}

	public void postPersist(ObjectNode node) {}

	@Override
	public String toString() {
		return name;
	}

	protected <F> NestedField<F> nest(Field<F> field) {
		return new NestedField<>(this, field);
	}

	protected static String internal(String name) {
		return '$' + name;
	}

	public static String concat(String parent, String field) {
		return parent + '.' + field;
	}
}
