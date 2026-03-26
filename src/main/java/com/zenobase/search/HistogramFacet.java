package com.zenobase.search;

import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.HistogramBucket;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.json.DecimalMeasureField;
import com.zenobase.json.Field;
import com.zenobase.json.Nodes;

public class HistogramFacet extends FilteredFacet {

	public static final String TYPE = "histogram";

	private final String field;
	private final double interval;
	private final Unit<?> unit;

	public HistogramFacet(String id, String field, double interval, Unit<?> unit, Query filter) {
		super(id, filter);
		this.field = field;
		this.interval = interval;
		this.unit = unit;
	}

	@Override
	public void configure(SearchRequest.Builder builder) {
		String f = unit == Unit.ONE ? this.field : Field.concat(this.field, DecimalMeasureField.VALUE_SI.getName());
		double stdInterval = getStandardInterval();
		double stdOffset = getStandardOffset();
		Aggregation histogram = Aggregation.of(
				a -> a.histogram(h -> h.field(f).interval(stdInterval).offset(stdOffset)));
		addAggregation(getId(), histogram, builder);
	}

	private double getStandardInterval() {
		if (unit == null || Units.isStandard(unit) || Units.C.equals(unit)) {
			return interval;
		}
		if (Units.F.equals(unit)) {
			return interval * (5.0 / 9.0);
		}
		return unit.toStandardUnit().convert(interval);
	}

	private double getStandardOffset() {
		if (Units.C.equals(unit)) {
			return 273.15 % interval;
		}
		if (Units.F.equals(unit)) {
			double zeroF_K = (-32.0) * 5.0 / 9.0 + 273.15;
			return zeroF_K % getStandardInterval();
		}
		return 0.0;
	}

	@Override
	public JsonNode process(SearchResponse<ObjectNode> response) {
		ArrayNode result = Nodes.newArray();
		Aggregate agg = getAggregate(response);
		for (HistogramBucket bucket : Lists.reverse(agg.histogram().buckets().array())) {
			if (bucket.docCount() > 0) {
				ObjectNode entryNode = result.addObject();
				entryNode.put("count", bucket.docCount());
				double key = bucket.key();
				addValue(entryNode, "from", key);
				addValue(entryNode, "to", key + getStandardInterval());
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
			return new HistogramFacet(
					options.get("id"),
					options.get("field"),
					options.get("interval", Double.class, 10.0),
					unit != null ? Units.valueOf(unit) : Unit.ONE,
					filterParser.parse(options.get("filter")));
		};
	}
}
