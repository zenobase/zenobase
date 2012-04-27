package com.zenobase.schema;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.IntNode;
import org.codehaus.jackson.node.ObjectNode;

import com.zenobase.models.Rating;

public class RatingField extends Field<Rating> {

	public RatingField(String name) {
		super(name, Rating.class, "byte");
	}

	@Override
	protected Rating getValue(JsonNode node) {
		return Rating.valueOf(node.getIntValue());
	}

	@Override
	protected JsonNode toJson(Rating value) {
		return new IntNode(value.getValue());
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("precision_step", "0");
	}
}
