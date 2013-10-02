package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.search.DecimalRangeConstraintBuilder;
import com.zenobase.search.TermConstraintBuilder;

public class IntegerField extends Field<Integer> {

	private final boolean indexed;

	public IntegerField(String name) {
		this(name, true);
	}

	public IntegerField(String name, boolean indexed) {
		super(name, Long.class, "integer");
		this.indexed = indexed;
		addConstraintBuilder(new DecimalRangeConstraintBuilder());
		addConstraintBuilder(new TermConstraintBuilder());
	}

	@Override
	protected Integer getValue(JsonNode node) {
		return node.intValue();
	}

	@Override
	public JsonNode toJson(Integer value) {
		return value != null ? new IntNode(value) : NullNode.getInstance();
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		if (!indexed) {
			schema.put("index", "no");
		}
	}
}
