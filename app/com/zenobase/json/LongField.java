package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.search.DecimalRangeConstraintBuilder;
import com.zenobase.search.TermConstraintBuilder;

public class LongField extends Field<Long> {

	private final boolean indexed;

	public LongField(String name) {
		this(name, true);
	}

	public LongField(String name, boolean indexed) {
		super(name, Long.class, "long");
		this.indexed = indexed;
		if (indexed) {
			addConstraintBuilder(name, new DecimalRangeConstraintBuilder(this));
			addConstraintBuilder(name, new TermConstraintBuilder(this));
		}
	}

	@Override
	protected Long getValue(JsonNode node) {
		return node.longValue();
	}

	@Override
	public JsonNode toJson(Long value) {
		return value != null ? new LongNode(value) : NullNode.getInstance();
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		if (!indexed) {
			schema.put("index", "no");
		}
	}
}
