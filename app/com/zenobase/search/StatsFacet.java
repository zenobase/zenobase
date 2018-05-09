package com.zenobase.search;

import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.stats.extended.ExtendedStats;
import org.elasticsearch.search.aggregations.metrics.stats.extended.ExtendedStatsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.json.DecimalMeasureField;
import com.zenobase.json.Field;
import com.zenobase.json.Nodes;

public class StatsFacet extends FilteredFacet {

	public static final String TYPE = "stats";

	private final String field;
	private final Unit<?> unit;

	public StatsFacet(String id, String field, Unit<?> unit, FilterBuilder filter) {
		super(id, filter);
		this.field = field;
		this.unit = unit;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		ExtendedStatsBuilder stats = AggregationBuilders.extendedStats(getId())
			.field(unit == Unit.ONE ? field : Field.concat(field, DecimalMeasureField.VALUE_SI.getName()));
		addAggregation(stats, builder);
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ExtendedStats stats = getAggregation(response);
		ObjectNode node = Nodes.newObject();
		node.put("count", stats.getCount());
		if (stats.getCount() > 0) {
			put(node, "min",  stats.getMin());
			put(node, "max",  stats.getMax());
			put(node, "sum",  stats.getSum());
			put(node, "avg",  stats.getAvg());
			put(node, "stdev", stats.getStdDeviation());
		}
		return node;
	}

	private void put(ObjectNode parent, String property, double value) {
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
					options.get("id"),
					options.get("field"),
					getUnit(options.get("unit")),
					filterParser.parse(options.get("filter")));
			}

			private Unit<?> getUnit(String value) {
				return value != null ? Units.valueOf(value) : Unit.ONE;
			}
		};
	}
}
