package com.zenobase.json;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;

import com.zenobase.models.Identity;
import com.zenobase.search.TermConstraintBuilder;

public class IdentityField extends Field<Identity> {

	public IdentityField(String name) {
		super(name, Identity.class, "string");
		addConstraintBuilder(new TermConstraintBuilder());
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
