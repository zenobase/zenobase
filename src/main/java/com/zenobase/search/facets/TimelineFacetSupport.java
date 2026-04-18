package com.zenobase.search.facets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.primitives.Doubles;
import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.json.DecimalMeasureField;
import com.zenobase.json.Field;
import com.zenobase.json.Nodes;
import javax.measure.unit.Unit;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch._types.query_dsl.Query;

abstract class TimelineFacetSupport extends FilteredFacet {

	protected final String keyField;
	protected final String valueField;
	protected final Unit<?> unit;

	protected TimelineFacetSupport(
		String id,
		String keyField,
		String valueField,
		Unit<?> unit,
		@Nullable Query filter
	) {
		super(id, filter);
		this.keyField = keyField;
		this.valueField = valueField;
		this.unit = unit;
	}

	protected String getField() {
		return Units.isDimensionless(unit)
			? valueField
			: Field.concat(valueField, DecimalMeasureField.VALUE_SI.getName());
	}

	protected JsonNode toJson(Iterable<ObjectNode> values) {
		ArrayNode node = Nodes.newArray();
		for (ObjectNode value : values) {
			node.add(value);
		}
		return node;
	}

	protected void addValue(ObjectNode parent, String property, @Nullable Double value) {
		if (value == null) {
			return;
		}
		if (Doubles.isFinite(value)) {
			if (!Units.isDimensionless(unit)) {
				ObjectNode node = parent.putObject(property);
				node.put("@value", Measures.convert(value, unit));
				node.put("unit", unit.toString());
			} else {
				parent.put(property, Measures.round(value));
			}
		}
	}
}
