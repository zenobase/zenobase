package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import com.zenobase.models.Identity;
import com.zenobase.search.TermConstraintBuilder;

public class IdentityField extends Field<Identity> {

	public IdentityField(String name) {
		this(name, null);
	}

	public IdentityField(String name, Field<?> parent) {
		super(name, Identity.class, "string", parent);
		addConstraintBuilder(name, new TermConstraintBuilder(getPath()));
	}

	@Override
	protected Identity getValue(JsonNode node) {
		return new Identity(node.asText());
	}

	@Override
	public JsonNode toJson(Identity value) {
		return value != null ? new TextNode(value.getId()) : NullNode.getInstance();
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("index", "not_analyzed");
	}
}
