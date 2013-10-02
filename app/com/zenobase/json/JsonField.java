package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonField extends Field<JsonNode> {

	public JsonField(String name) {
		super(name, JsonNode.class, "object");
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("enabled", false);
	}

	@Override
	protected JsonNode getValue(JsonNode node) {
		return node;
	}

	@Override
	public JsonNode toJson(JsonNode value) {
		return value != null ? value : NullNode.getInstance();
	}
}
