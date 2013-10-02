package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import com.zenobase.search.PhraseConstraintBuilder;
import com.zenobase.search.TermConstraintBuilder;
import com.zenobase.search.WildcardConstraintBuilder;

public class TextField extends Field<String> {

	public TextField(String name) {
		super(name, String.class, "string");
		addConstraintBuilder(new PhraseConstraintBuilder());
		addConstraintBuilder(new WildcardConstraintBuilder());
		addConstraintBuilder(new TermConstraintBuilder());
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
		schema.put("index", "analyzed");
	}
}
