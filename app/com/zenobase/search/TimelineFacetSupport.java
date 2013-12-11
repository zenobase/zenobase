package com.zenobase.search;

import javax.measure.unit.Unit;

import org.elasticsearch.index.query.FilterBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.common.Measures;
import com.zenobase.json.Nodes;

abstract class TimelineFacetSupport extends Facet {

	protected final String keyField;
	protected final String valueField;
	protected final Unit<?> unit;
	protected final FilterBuilder filter;

	protected TimelineFacetSupport(String id, String keyField, String valueField, Unit<?> unit, FilterBuilder filter) {
		super(id);
		this.keyField = keyField;
		this.valueField = valueField;
		this.unit = unit;
		this.filter = filter;
	}

	protected JsonNode toJson(Iterable<ObjectNode> values) {
		ArrayNode node = Nodes.newArray();
		for (ObjectNode value : values) {
			node.add(value);
		}
		return node;
	}

	protected void addValue(ObjectNode parent, String property, double value) {
		if (unit != Unit.ONE) {
			ObjectNode node = parent.putObject(property);
			node.put("@value", Measures.convert(value, unit));
			node.put("unit", unit.toString());
		} else {
			parent.put(property, Measures.round(value));
		}
	}
}
