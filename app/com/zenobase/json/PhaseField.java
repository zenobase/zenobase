package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.NullNode;

import com.zenobase.models.Phase;
import com.zenobase.search.DecimalRangeConstraintBuilder;
import com.zenobase.search.TermConstraintBuilder;

public class PhaseField extends Field<Phase> {

	public PhaseField(String name) {
		super(name, Phase.class, "float");
		addConstraintBuilder(name, new DecimalRangeConstraintBuilder(getPath()));
		addConstraintBuilder(name, new TermConstraintBuilder(getPath()));
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
