package com.zenobase.json;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JsonSchema(
		String type, @Nullable String format, @Nullable Map<String, JsonSchema> properties) {

	public static JsonSchema string() {
		return new JsonSchema("string", null, null);
	}

	public static JsonSchema string(String format) {
		return new JsonSchema("string", format, null);
	}

	public static JsonSchema integer() {
		return new JsonSchema("integer", null, null);
	}

	public static JsonSchema number() {
		return new JsonSchema("number", null, null);
	}

	public static JsonSchema object(Map<String, JsonSchema> properties) {
		return new JsonSchema("object", null, properties);
	}

	public static JsonSchema forFields(Iterable<Field<?>> fields) {
		Map<String, JsonSchema> properties = new LinkedHashMap<>();
		for (Field<?> field : fields) {
			properties.put(field.getName(), field.toJsonSchema());
		}
		return object(properties);
	}

	public ObjectNode toJson() {
		return Nodes.MAPPER.valueToTree(this);
	}
}
