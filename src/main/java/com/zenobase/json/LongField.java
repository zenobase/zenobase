package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;

import com.zenobase.search.constraints.DecimalRangeConstraintBuilder;
import com.zenobase.search.constraints.TermConstraintBuilder;

public class LongField extends Field<Long> {

	private final boolean indexed;

	public LongField(String name) {
		this(name, true);
	}

	public LongField(String name, boolean indexed) {
		super(name, Long.class, "long");
		this.indexed = indexed;
		if (indexed) {
			addConstraintBuilder(name, new DecimalRangeConstraintBuilder(getPath()));
			addConstraintBuilder(name, new TermConstraintBuilder(getPath()));
		}
	}

	@Override
	protected Long getValue(JsonNode node) {
		return node.longValue();
	}

	@Override
	public JsonNode toJson(@Nullable Long value) {
		return value != null ? new LongNode(value) : NullNode.getInstance();
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		if (!indexed) {
			schema.put("index", false);
		}
	}
}
