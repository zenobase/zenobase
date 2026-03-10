package com.zenobase.search;

import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.common.geo.GeoPoint;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.geogrid.GeoGrid;
import org.opensearch.search.aggregations.metrics.Sum;
import org.opensearch.search.aggregations.metrics.SumAggregationBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;

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

	private HeatmapFacet(String id, String keyField, String valueField, Unit<?> unit, int precision, QueryBuilder filter) {
		super(id, filter);
		Preconditions.checkArgument(precision >= 1 && precision <= 10, "invalid precision value: %d", precision);
		this.keyField = keyField;
		this.valueField = valueField;
		this.unit = unit;
		this.precision = precision;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		AggregationBuilder grid = AggregationBuilders.geohashGrid(getId()).field(keyField).precision(precision);
		if (valueField != null) {
			grid.subAggregation(new SumAggregationBuilder("sum").field(unit == Unit.ONE ? valueField : Field.concat(valueField, DecimalMeasureField.VALUE_SI.getName())));
		}
		addAggregation(grid, builder);
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode result = Nodes.newArray();
		GeoGrid grid = getAggregation(response);
		for (GeoGrid.Bucket bucket : grid.getBuckets()) {
			Sum sum = bucket.getAggregations().get("sum");
			if (sum == null || sum.getValue() > 0.0) {
				ObjectNode entryNode = result.addObject();
				GeoPoint point = GeoPoint.fromGeohash(bucket.getKeyAsString());
				entryNode.put("lat", point.lat());
				entryNode.put("lon", point.lon());
				entryNode.put("count", bucket.getDocCount());
				if (sum != null) {
					addValue(entryNode, "sum", sum.getValue());
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
