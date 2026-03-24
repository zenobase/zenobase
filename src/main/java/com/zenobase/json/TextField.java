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
		this(name, name);
	}

	public TextField(String path, String name) {
		super(path, name, String.class, "text");
		addConstraintBuilder(path, new PhraseConstraintBuilder(path));
		addConstraintBuilder(path, new WildcardConstraintBuilder(path));
		addConstraintBuilder(path, new TermConstraintBuilder(path));
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
	}
}
