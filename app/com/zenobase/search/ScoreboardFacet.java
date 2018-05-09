package com.zenobase.search;

import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.stats.extended.ExtendedStats;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.json.DecimalMeasureField;
import com.zenobase.json.Field;
import com.zenobase.json.Nodes;

public class ScoreboardFacet extends FilteredFacet {

	public static final String TYPE = "scoreboard";

	private final String termField;
	private final String valueField;
	private final Unit<?> unit;
	private final Terms.Order order;
	private final int limit;

	private ScoreboardFacet(String id, String termField, String valueField, Unit<?> unit, String order, int limit, FilterBuilder filter) {
		super(id, filter);
		this.termField = termField;
		this.valueField = valueField;
		this.unit = unit;
		this.order = parseOrder(order);
		this.limit = limit;
	}

	private Terms.Order parseOrder(String s) {
		boolean asc = !s.startsWith("-");
		if (!asc) {
			s = s.substring(1);
		}
		if ("count".equals(s)) {
			return Terms.Order.count(asc);
		} else if ("term".equals(s)) {
			return Terms.Order.term(asc);
		} else {
			return Terms.Order.aggregation(getId(), s, asc);
		}
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		AggregationBuilder<?> terms = AggregationBuilders.terms(getId())
			.field(termField).order(order).size(limit)
			.subAggregation(AggregationBuilders.extendedStats(getId()).field(getValueField()));
		addAggregation(terms, builder);
	}

	private String getValueField() {
		return unit == Unit.ONE ? valueField : Field.concat(valueField, DecimalMeasureField.VALUE_SI.getName());
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode result = Nodes.newArray();
		Terms terms = getAggregation(response);
		for (Terms.Bucket bucket : terms.getBuckets()) {
			ExtendedStats stats = bucket.getAggregations().get(getId());
			if (stats.getCount() > 0) {
				ObjectNode entryNode = result.addObject();
				entryNode.put("label", bucket.getKey());
				entryNode.put("count", bucket.getDocCount());
				addValue(entryNode, "min", stats.getMin());
				addValue(entryNode, "max", stats.getMax());
				addValue(entryNode, "sum", stats.getSum());
				addValue(entryNode, "avg", stats.getAvg());
			}
		}
		return result;
	}

	private void addValue(ObjectNode parent, String property, double value) {
		if (unit != Unit.ONE) {
			ObjectNode node = parent.putObject(property);
			node.put("@value", Measures.convert(value, unit));
			node.put("unit", unit.toString());
		} else {
			parent.put(property, Measures.round(value));
		}
	}

	public static FacetBuilder builder(FilterParser filterParser) {
		return options -> {
			String unit = options.get("unit");
			return new ScoreboardFacet(
				options.get("id"),
				options.get("key_field"),
				options.get("value_field"),
				unit != null ? Units.valueOf(unit) : Unit.ONE,
				options.get("order", String.class, "-count"),
				options.get("limit", Integer.class, 10),
				filterParser.parse(options.get("filter")));
		};
	}
}
