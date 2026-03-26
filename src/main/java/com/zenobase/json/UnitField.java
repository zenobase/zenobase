package com.zenobase.json;

import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.jspecify.annotations.Nullable;

import com.zenobase.common.Units;

public class UnitField<Q extends Quantity> extends Field<Unit<Q>> {

	public UnitField(String name) {
		super(name, String.class, "keyword");
	}

	@Override
	protected @Nullable Unit<Q> getValue(JsonNode node) {
		if (!node.isTextual()) {
			return null;
		}
		return Units.valueOf(node.textValue());
	}

	@Override
	public JsonNode toJson(@Nullable Unit<Q> value) {
		return value != null ? new TextNode(value.toString()) : NullNode.getInstance();
	}
}
