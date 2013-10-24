package com.zenobase.json;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.NullNode;

public class DecimalField extends Field<BigDecimal> {

	public DecimalField(String name) {
		super(name, BigDecimal.class, "double");
	}

	@Override
	protected BigDecimal getValue(JsonNode node) {
		return node.decimalValue();
	}

	@Override
	public JsonNode toJson(BigDecimal value) {
		return value != null ? new DecimalNode(value) : NullNode.getInstance();
	}
}
