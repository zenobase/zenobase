package com.zenobase.json;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.BooleanNode;
import org.codehaus.jackson.node.NullNode;

import com.zenobase.search.TermConstraint;

public class BooleanField extends Field<Boolean> {

	public BooleanField(String name) {
		super(name, Boolean.class, "boolean");
		addConstraint(new TermConstraint());
	}

	@Override
	protected Boolean getValue(JsonNode node) {
		return node.getBooleanValue();
	}

	@Override
	public JsonNode toJson(Boolean value) {
		return value != null ? BooleanNode.valueOf(value) : NullNode.getInstance();
	}
}
