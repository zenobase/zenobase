package com.zenobase.schema;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;

public class TextField extends Field<String> {

	public TextField(String name) {
		super(name, String.class, "string");
	}

	@Override
	protected String getValue(JsonNode node) {
		return node.asText();
	}

	@Override
	protected JsonNode toJson(String value) {
		return new TextNode(value);
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("index", "analyzed");
	}
}
