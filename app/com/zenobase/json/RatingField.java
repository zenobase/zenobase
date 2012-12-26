package com.zenobase.json;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.IntNode;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.ObjectNode;

import com.zenobase.models.Rating;
import com.zenobase.search.DecimalRangeConstraint;
import com.zenobase.search.TermConstraint;

public class RatingField extends Field<Rating> {

	public RatingField(String name) {
		super(name, Rating.class, "byte");
		addConstraint(new DecimalRangeConstraint());
		addConstraint(new TermConstraint());
	}

	@Override
	protected Rating getValue(JsonNode node) {
		return Rating.valueOf(node.getIntValue());
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
