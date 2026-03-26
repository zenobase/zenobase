package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;

import com.zenobase.models.Rating;
import com.zenobase.search.ExistsConstraintBuilder;
import com.zenobase.search.PercentConstraintBuilder;
import com.zenobase.search.PercentRangeConstraintBuilder;

public class RatingField extends Field<Rating> {

	public RatingField(String name) {
		super(name, Rating.class, "byte");
		addConstraintBuilder(name, new ExistsConstraintBuilder(getPath()));
		addConstraintBuilder(name, new PercentRangeConstraintBuilder(getPath()));
		addConstraintBuilder(name, new PercentConstraintBuilder(getPath()));
	}

	@Override
	protected Rating getValue(JsonNode node) {
		return Rating.valueOf(node.intValue());
	}

	@Override
	public JsonNode toJson(@Nullable Rating value) {
		return value != null ? new IntNode(value.value()) : NullNode.getInstance();
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
	}
}
