package com.zenobase.json;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;

public class TokenField extends Field<String> {

	private final boolean indexed;

	public TokenField(String name) {
		this(name, true);
	}

	public TokenField(String name, boolean indexed) {
		super(name, String.class, "string");
		this.indexed = indexed;
	}

	@Override
	protected String getValue(JsonNode node) {
		return node.getTextValue();
	}

	@Override
	public JsonNode toJson(String value) {
		return value != null ? new TextNode(value) : NullNode.getInstance();
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("index", indexed ? "not_analyzed" : "no");
	}
}
