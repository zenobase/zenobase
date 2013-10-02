package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import com.zenobase.search.TermConstraintBuilder;
import com.zenobase.search.WildcardConstraintBuilder;

public class TokenField extends Field<String> {

	private final boolean indexed;

	public TokenField(String name) {
		this(name, true);
	}

	public TokenField(String name, boolean indexed) {
		super(name, String.class, "string");
		this.indexed = indexed;
		if (indexed) {
			addConstraintBuilder(new WildcardConstraintBuilder());
			addConstraintBuilder(new TermConstraintBuilder());
		}
	}

	@Override
	protected String getValue(JsonNode node) {
		return node.textValue();
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
