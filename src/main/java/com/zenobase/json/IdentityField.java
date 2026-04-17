package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.jspecify.annotations.Nullable;

import com.zenobase.models.Identity;
import com.zenobase.search.constraints.TermConstraintBuilder;

public class IdentityField extends Field<Identity> {

	public IdentityField(String name) {
		super(name, Identity.class, "keyword");
		addConstraintBuilder(name, new TermConstraintBuilder(getPath()));
	}

	@Override
	protected Identity getValue(JsonNode node) {
		return new Identity(node.asText());
	}

	@Override
	public JsonNode toJson(@Nullable Identity value) {
		return value != null ? new TextNode(value.id()) : NullNode.getInstance();
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
	}
}
