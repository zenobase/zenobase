package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.NullNode;

import com.zenobase.models.Phase;
import com.zenobase.search.PhaseConstraintBuilder;
import com.zenobase.search.PhaseRangeConstraintBuilder;

public class PhaseField extends Field<Phase> {

	public PhaseField(String name) {
		super(name, Phase.class, "float");
		addConstraintBuilder(name, new PhaseRangeConstraintBuilder(getPath()));
		addConstraintBuilder(name, new PhaseConstraintBuilder(getPath()));
	}

	@Override
	protected Phase getValue(JsonNode node) {
		return Phase.valueOf(node.decimalValue());
	}

	@Override
	public JsonNode toJson(Phase value) {
		return value != null ? new DecimalNode(value.getValue()) : NullNode.getInstance();
	}
}
