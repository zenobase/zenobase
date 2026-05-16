package com.zenobase.mcp.tools;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.Nodes;
import java.util.LinkedHashMap;
import java.util.Map;

/** Small builder for the JSON Schema payloads tools advertise via {@code tools/list}. */
final class ToolSchemas {

	private ToolSchemas() {}

	static ObjectNode bucketIdAnd(Map<String, ObjectNode> extra, String... required) {
		Map<String, ObjectNode> props = new LinkedHashMap<>();
		props.put(
			"bucket_id",
			stringProperty("ID of the bucket to query (the {id} from a zenobase://bucket/{id} URI).")
		);
		props.putAll(extra);
		props.put("constraints", constraintsProperty());
		ObjectNode schema = Nodes.newObject();
		schema.put("type", "object");
		ObjectNode properties = schema.putObject("properties");
		for (Map.Entry<String, ObjectNode> entry : props.entrySet()) {
			properties.set(entry.getKey(), entry.getValue());
		}
		ArrayNode requiredArray = schema.putArray("required");
		requiredArray.add("bucket_id");
		for (String name : required) {
			requiredArray.add(name);
		}
		return schema;
	}

	static ObjectNode stringProperty(String description) {
		ObjectNode node = Nodes.newObject();
		node.put("type", "string");
		node.put("description", description);
		return node;
	}

	static ObjectNode integerProperty(String description, int minimum, int maximum) {
		ObjectNode node = Nodes.newObject();
		node.put("type", "integer");
		node.put("minimum", minimum);
		node.put("maximum", maximum);
		node.put("description", description);
		return node;
	}

	private static ObjectNode constraintsProperty() {
		ObjectNode constraints = Nodes.newObject();
		constraints.put("type", "array");
		constraints.put(
			"description",
			"Optional list of AND-combined predicates. Each: {field, op, value}. " +
				"Ops: eq, ne, gt, gte, lt, lte, in (value=array), contains (substring). " +
				"Field names come from the bucket's schema (resource zenobase://bucket/{id})."
		);
		ObjectNode items = constraints.putObject("items");
		items.put("type", "object");
		ObjectNode itemProps = items.putObject("properties");
		itemProps.set("field", stringProperty("Field name from the bucket schema."));
		itemProps.set(
			"op",
			enumProperty("Comparison operator.", "eq", "ne", "gt", "gte", "lt", "lte", "in", "contains")
		);
		ObjectNode value = Nodes.newObject();
		value.put("description", "Scalar (string/number/bool) or, for 'in' op, an array.");
		itemProps.set("value", value);
		ArrayNode req = items.putArray("required");
		req.add("field");
		req.add("op");
		req.add("value");
		return constraints;
	}

	static ObjectNode enumProperty(String description, String... values) {
		ObjectNode node = Nodes.newObject();
		node.put("type", "string");
		node.put("description", description);
		ArrayNode array = node.putArray("enum");
		for (String value : values) {
			array.add(value);
		}
		return node;
	}
}
