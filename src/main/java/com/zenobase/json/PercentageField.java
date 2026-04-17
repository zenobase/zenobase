package com.zenobase.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.jspecify.annotations.Nullable;

import com.zenobase.models.Percentage;
import com.zenobase.search.constraints.ExistsConstraintBuilder;
import com.zenobase.search.constraints.PercentConstraintBuilder;
import com.zenobase.search.constraints.PercentRangeConstraintBuilder;

public class PercentageField extends Field<Percentage> {

	public PercentageField(String name) {
		super(name, Percentage.class, "float");
		addConstraintBuilder(name, new ExistsConstraintBuilder(getPath()));
		addConstraintBuilder(name, new PercentRangeConstraintBuilder(getPath()));
		addConstraintBuilder(name, new PercentConstraintBuilder(getPath()));
	}

	@Override
	protected Percentage getValue(JsonNode node) {
		return Percentage.valueOf(node.decimalValue());
	}

	@Override
	public JsonNode toJson(@Nullable Percentage value) {
		return value != null ? new DecimalNode(value.value()) : NullNode.getInstance();
	}
}
