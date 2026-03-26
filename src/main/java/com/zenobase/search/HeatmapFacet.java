package com.zenobase.search;

import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.io.GeohashUtils;
import org.locationtech.spatial4j.shape.Point;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.GeoHashGridBucket;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

import com.zenobase.common.Measures;
import com.zenobase.common.Units;
import com.zenobase.json.DecimalMeasureField;
import com.zenobase.json.Field;
import com.zenobase.json.Nodes;
import com.zenobase.models.Event;

public class HeatmapFacet extends FilteredFacet {

	public static final String TYPE = "heatmap";

	private final String keyField;
	private final String valueField;
	private final Unit<?> unit;
	private final int precision;

	private HeatmapFacet(String id, String keyField, String valueField, Unit<?> unit, int precision, Query filter) {
		super(id, filter);
		Preconditions.checkArgument(precision >= 1 && precision <= 10, "invalid precision value: %d", precision);
		this.keyField = keyField;
		this.valueField = valueField;
		this.unit = unit;
		this.precision = precision;
	}

	@Override
	public void configure(SearchRequest.Builder builder) {
		String vf = valueField;
		Aggregation aggregation;
		if (vf != null) {
			String sumField = unit == Unit.ONE ? vf : Field.concat(vf, DecimalMeasureField.VALUE_SI.getName());
			aggregation =
					Aggregation.of(a -> a.geohashGrid(g -> g.field(keyField).precision(p -> p.geohashLength(precision)))
							.aggregations("sum", Aggregation.of(sa -> sa.sum(s -> s.field(sumField)))));
		} else {
			aggregation = Aggregation.of(
					a -> a.geohashGrid(g -> g.field(keyField).precision(p -> p.geohashLength(precision))));
		}
		addAggregation(getId(), aggregation, builder);
	}

	@Override
	public JsonNode process(SearchResponse<ObjectNode> response) {
		ArrayNode result = Nodes.newArray();
		Aggregate agg = getAggregate(response);
		for (GeoHashGridBucket bucket : agg.geohashGrid().buckets().array()) {
			Aggregate sumAgg = bucket.aggregations().get("sum");
			double sumValue = sumAgg != null ? sumAgg.sum().value() : -1;
			if (sumAgg == null || sumValue > 0.0) {
				ObjectNode entryNode = result.addObject();
				Point point = GeohashUtils.decode(bucket.key(), SpatialContext.GEO);
				entryNode.put("lat", point.getY());
				entryNode.put("lon", point.getX());
				entryNode.put("count", bucket.docCount());
				if (sumAgg != null) {
					addValue(entryNode, "sum", sumValue);
				}
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
			return new HeatmapFacet(
					options.get("id"),
					options.get("field", String.class, Event.LOCATION.getName()),
					options.get("value_field", String.class, null),
					unit != null ? Units.valueOf(unit) : Unit.ONE,
					options.get("precision", Integer.class, 8),
					filterParser.parse(options.get("filter")));
		};
	}
}
