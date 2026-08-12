package com.zenobase.mcp.tools;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the JSON Schemas tools advertise via {@code tools/list}. Implemented directly with {@link jakarta.json}
 * builders rather than {@link io.helidon.json.schema.Schema} because the latter doesn't model two things we need:
 * {@code enum} (one of a fixed set of strings) and "any" type (for the {@code constraints[].value} field, which is
 * legitimately a scalar or array). Returning the schema as a string is exactly what {@code McpTool.schema()} expects.
 */
final class ToolSchemas {

	private ToolSchemas() {}

	/** Standard property: a bucket-scoped tool's required {@code bucket_id} string. */
	static JsonObject bucketIdProperty() {
		return stringProperty("ID of the bucket to query (the {id} from a zenobase://bucket/{id} URI).");
	}

	static JsonObject stringProperty(String description) {
		return Json.createObjectBuilder().add("type", "string").add("description", description).build();
	}

	static JsonObject stringPropertyWithEnum(String description, String... values) {
		JsonArrayBuilder enumArray = Json.createArrayBuilder();
		for (String value : values) {
			enumArray.add(value);
		}
		return Json.createObjectBuilder()
			.add("type", "string")
			.add("description", description)
			.add("enum", enumArray)
			.build();
	}

	static JsonObject integerProperty(String description, int minimum, int maximum) {
		return Json.createObjectBuilder()
			.add("type", "integer")
			.add("description", description)
			.add("minimum", minimum)
			.add("maximum", maximum)
			.build();
	}

	/**
	 * The constraints array advertised by all facet-backed tools. Each element is {@code {field, op, value}} where
	 * {@code value} is intentionally untyped — strings, numbers, booleans, and arrays (for the {@code in} op) are all
	 * legal. JSON Schema represents "any type" by simply omitting {@code type}.
	 */
	static JsonObject constraintsProperty() {
		JsonObjectBuilder itemProps = Json.createObjectBuilder()
			.add("field", stringProperty("Field name from the bucket schema."))
			.add(
				"op",
				stringPropertyWithEnum("Comparison operator.", "eq", "ne", "gt", "gte", "lt", "lte", "in", "contains")
			)
			.add(
				"value",
				Json.createObjectBuilder()
					.add("description", "Scalar (string/number/bool) or, for 'in' op, an array.")
					.build()
			);
		JsonObject items = Json.createObjectBuilder()
			.add("type", "object")
			.add("properties", itemProps)
			.add("required", Json.createArrayBuilder().add("field").add("op").add("value"))
			.build();
		return Json.createObjectBuilder()
			.add("type", "array")
			.add(
				"description",
				"Optional list of AND-combined predicates. Each: {field, op, value}. " +
					"Ops: eq, ne, gt, gte, lt, lte, in (value=array), contains (substring). " +
					"Field names come from the bucket's schema (resource zenobase://bucket/{id})."
			)
			.add("items", items)
			.build();
	}

	/**
	 * Composes a schema with a required {@code bucket_id}, optional {@code constraints}, and the supplied extra
	 * properties. Names in {@code extraRequired} are added to the {@code required} array.
	 *
	 * @param extras           ordered map of additional property name → property schema
	 * @param extraRequired    names of extra properties that should also be required
	 */
	static String bucketIdAnd(Map<String, JsonObject> extras, String... extraRequired) {
		Map<String, JsonObject> props = new LinkedHashMap<>();
		props.put("bucket_id", bucketIdProperty());
		props.putAll(extras);
		props.put("constraints", constraintsProperty());

		JsonObjectBuilder properties = Json.createObjectBuilder();
		for (Map.Entry<String, JsonObject> entry : props.entrySet()) {
			properties.add(entry.getKey(), entry.getValue());
		}
		JsonArrayBuilder required = Json.createArrayBuilder().add("bucket_id");
		for (String name : extraRequired) {
			required.add(name);
		}
		return Json.createObjectBuilder()
			.add("type", "object")
			.add("properties", properties)
			.add("required", required)
			.build()
			.toString();
	}

	/** Like {@link #bucketIdAnd} but without the {@code constraints} array (e.g. for the {@code schema} tool). */
	static String bucketIdOnly() {
		return Json.createObjectBuilder()
			.add("type", "object")
			.add("properties", Json.createObjectBuilder().add("bucket_id", bucketIdProperty()))
			.add("required", Json.createArrayBuilder().add("bucket_id"))
			.build()
			.toString();
	}

	/** Schema for tools that take no input. */
	static String empty() {
		return Json.createObjectBuilder()
			.add("type", "object")
			.add("properties", Json.createObjectBuilder())
			.build()
			.toString();
	}
}
