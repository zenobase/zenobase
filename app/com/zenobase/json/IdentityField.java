package com.zenobase.json;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;

import com.zenobase.models.Identity;

public class IdentityField extends Field<Identity> {

	public IdentityField(String name) {
		super(name, Identity.class, "string");
	}

	@Override
	protected Identity getValue(JsonNode node) {
		return new Identity(node.asText());
	}

	@Override
	protected JsonNode toJson(Identity value) {
		return new TextNode(value.getId());
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("index", "not_analyzed");
	}
}
