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
		this(null, name);
	}

	public TextField(Field<?> parent, String name) {
		super(parent, name, String.class, "string");
		addConstraintBuilder(name, new PhraseConstraintBuilder(getPath()));
		addConstraintBuilder(name, new WildcardConstraintBuilder(getPath()));
		addConstraintBuilder(name, new TermConstraintBuilder(getPath()));
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
