package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Rating;
import com.zenobase.search.PercentConstraintBuilder;
import com.zenobase.search.PercentRangeConstraintBuilder;

public class RatingField extends Field<Rating> {

	public RatingField(String name) {
		super(name, Rating.class, "byte");
		addConstraintBuilder(name, new PercentRangeConstraintBuilder(this));
		addConstraintBuilder(name, new PercentConstraintBuilder(this));
	}

	@Override
	protected Rating getValue(JsonNode node) {
		return Rating.valueOf(node.intValue());
	}

	@Override
	public JsonNode toJson(Rating value) {
		return value != null ? new IntNode(value.getValue()) : NullNode.getInstance();
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("precision_step", "0");
	}
}
