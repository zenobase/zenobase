package com.zenobase.mcp;

import com.zenobase.json.JsonSchema;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import java.util.Map;

/**
 * Serializes {@link JsonSchema} records into {@code jakarta.json} trees so the MCP package doesn't have to round-trip
 * through Jackson via {@link JsonSchema#toJson()}. Mirrors {@code @JsonInclude(NON_NULL)} on the record — null fields
 * are omitted.
 *
 * <p>Lives in the MCP package on purpose: we want to confine {@code jakarta.json} to the MCP surface for now. When the
 * rest of the codebase migrates off Jackson, this can move (or fold) into {@link JsonSchema} itself.
 */
public final class JsonSchemas {

	private JsonSchemas() {}

	public static JsonObject toJsonObject(JsonSchema schema) {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		if (schema.type() != null) {
			builder.add("type", schema.type());
		}
		if (schema.format() != null) {
			builder.add("format", schema.format());
		}
		if (schema.properties() != null) {
			JsonObjectBuilder props = Json.createObjectBuilder();
			for (Map.Entry<String, JsonSchema> entry : schema.properties().entrySet()) {
				props.add(entry.getKey(), toJsonObject(entry.getValue()));
			}
			builder.add("properties", props);
		}
		if (schema.items() != null) {
			builder.add("items", toJsonObject(schema.items()));
		}
		if (schema.oneOf() != null) {
			JsonArrayBuilder array = Json.createArrayBuilder();
			for (JsonSchema alt : schema.oneOf()) {
				array.add(toJsonObject(alt));
			}
			builder.add("oneOf", array);
		}
		if (schema.readOnly() != null) {
			builder.add("readOnly", schema.readOnly());
		}
		return builder.build();
	}
}
