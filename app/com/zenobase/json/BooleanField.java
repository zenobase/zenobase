package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NullNode;

import com.zenobase.search.TermConstraintBuilder;

public class BooleanField extends Field<Boolean> {

	public BooleanField(String name) {
		super(name, Boolean.class, "boolean");
		addConstraintBuilder(name, new TermConstraintBuilder(getPath()));
	}

	@Override
	protected Boolean getValue(JsonNode node) {
		return node.booleanValue();
	}

	@Override
	public JsonNode toJson(Boolean value) {
		return value != null ? BooleanNode.valueOf(value) : NullNode.getInstance();
	}
}
