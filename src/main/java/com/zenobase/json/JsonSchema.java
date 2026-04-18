package com.zenobase.json;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JsonSchema(
		@Nullable String type,
		@Nullable String format,
		@Nullable Map<String, JsonSchema> properties,
		@Nullable JsonSchema items,
		@Nullable List<JsonSchema> oneOf,
		@Nullable Boolean readOnly) {

	public static JsonSchema string() {
		return new JsonSchema("string", null, null, null, null, null);
	}

	public static JsonSchema string(String format) {
		return new JsonSchema("string", format, null, null, null, null);
	}

	public static JsonSchema integer() {
		return new JsonSchema("integer", null, null, null, null, null);
	}

	public static JsonSchema number() {
		return new JsonSchema("number", null, null, null, null, null);
	}

	public static JsonSchema object(Map<String, JsonSchema> properties) {
		return new JsonSchema("object", null, properties, null, null, null);
	}

	public static JsonSchema array(JsonSchema items) {
		return new JsonSchema("array", null, null, items, null, null);
	}

	public static JsonSchema oneOf(JsonSchema... alternatives) {
		return new JsonSchema(null, null, null, null, List.of(alternatives), null);
	}

	public JsonSchema asReadOnly() {
		return new JsonSchema(type, format, properties, items, oneOf, true);
	}

	public static JsonSchema forFields(Iterable<Field<?>> fields, Set<Field<?>> readOnly) {
		Map<String, JsonSchema> properties = new LinkedHashMap<>();
		for (Field<?> field : fields) {
			JsonSchema base = field.toJsonSchema();
			JsonSchema entry = readOnly.contains(field) ? base.asReadOnly() : oneOf(base, array(base));
			properties.put(field.getName(), entry);
		}
		return object(properties);
	}

	public ObjectNode toJson() {
		return Nodes.MAPPER.valueToTree(this);
	}
}
