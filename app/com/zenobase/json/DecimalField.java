package com.zenobase.json;

import java.math.BigDecimal;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.DecimalNode;
import org.codehaus.jackson.node.NullNode;

public class DecimalField extends Field<BigDecimal> {

	public DecimalField(String name) {
		super(name, BigDecimal.class, "double");
	}

	@Override
	protected BigDecimal getValue(JsonNode node) {
		return node.getDecimalValue();
	}

	@Override
	public JsonNode toJson(BigDecimal value) {
		return value != null ? new DecimalNode(value) : NullNode.getInstance();
	}
}
