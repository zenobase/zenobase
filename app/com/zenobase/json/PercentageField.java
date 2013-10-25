package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.NullNode;

import com.zenobase.models.Percentage;
import com.zenobase.search.PercentConstraintBuilder;
import com.zenobase.search.PercentRangeConstraintBuilder;

public class PercentageField extends Field<Percentage> {

	public PercentageField(String name) {
		super(name, Percentage.class, "float");
		addConstraintBuilder(name, new PercentRangeConstraintBuilder(getPath()));
		addConstraintBuilder(name, new PercentConstraintBuilder(getPath()));
	}

	@Override
	protected Percentage getValue(JsonNode node) {
		return Percentage.valueOf(node.decimalValue());
	}

	@Override
	public JsonNode toJson(Percentage value) {
		return value != null ? new DecimalNode(value.getValue()) : NullNode.getInstance();
	}
}
