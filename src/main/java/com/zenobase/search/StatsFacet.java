package com.zenobase.search;

import java.util.Objects;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.json.DecimalMeasureField;
import com.zenobase.json.Field;
import com.zenobase.json.Nodes;

public class StatsFacet extends FilteredFacet {

	public static final String TYPE = "stats";

	private final String field;
	private final Unit<?> unit;

	public StatsFacet(String id, String field, Unit<?> unit, @Nullable Query filter) {
		super(id, filter);
		this.field = field;
		this.unit = unit;
	}

	@Override
	public void configure(SearchRequest.Builder builder) {
		String f = unit == Unit.ONE ? field : Field.concat(field, DecimalMeasureField.VALUE_SI.getName());
		Aggregation stats = Aggregation.of(a -> a.extendedStats(e -> e.field(f)));
		addAggregation(getId(), stats, builder);
	}

	@Override
	public JsonNode process(SearchResponse<ObjectNode> response) {
		var aggregate = getAggregate(response);
		var node = Nodes.newObject();
		if (aggregate == null) {
			return node;
		}
		var stats = aggregate.extendedStats();
		node.put("count", stats.count());
		if (stats.count() > 0) {
			put(node, "min", stats.min());
			put(node, "max", stats.max());
			put(node, "sum", stats.sum());
			put(node, "avg", stats.avg());
			put(node, "stdev", stats.stdDeviation());
		}
		return node;
	}

	private void put(ObjectNode parent, String property, @Nullable Double value) {
		if (value == null) {
			return;
		}
		if (unit != Unit.ONE) {
			ObjectNode node = parent.putObject(property);
			node.put("@value", Measures.convert(value, unit));
			node.put("unit", unit.toString());
		} else {
			parent.put(property, Measures.round(value));
		}
	}

	public static FacetBuilder builder(FilterParser filterParser) {

		return new FacetBuilder() {

			@Override
			public Facet build(FacetOptions options) {
				return new StatsFacet(
						Objects.requireNonNull(options.get("id")),
						Objects.requireNonNull(options.get("field")),
						getUnit(options.get("unit")),
						filterParser.parse(options.get("filter")));
			}

			private Unit<?> getUnit(@Nullable String value) {
				return value != null ? Units.valueOf(value) : Unit.ONE;
			}
		};
	}
}
