package com.zenobase.search;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import org.jspecify.annotations.Nullable;
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
	private final @Nullable String valueField;
	private final Unit<?> unit;
	private final int precision;

	private HeatmapFacet(
			String id,
			String keyField,
			@Nullable String valueField,
			Unit<?> unit,
			int precision,
			@Nullable Query filter) {
		super(id, filter);
		Preconditions.checkArgument(precision >= 1 && precision <= 10, "invalid precision value: %s", precision);
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
			String sumField =
					Units.isDimensionless(unit) ? vf : Field.concat(vf, DecimalMeasureField.VALUE_SI.getName());
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
		Aggregate aggregate = getAggregate(response);
		if (aggregate == null) {
			return result;
		}
		for (GeoHashGridBucket bucket : aggregate.geohashGrid().buckets().array()) {
			var sumValue = Optional.ofNullable(bucket.aggregations().get("sum"))
					.map(a -> a.sum().value())
					.map(OptionalDouble::of)
					.orElse(OptionalDouble.empty());
			if (sumValue.isEmpty() || sumValue.getAsDouble() > 0.0) {
				ObjectNode entryNode = result.addObject();
				Point point = GeohashUtils.decode(bucket.key(), SpatialContext.GEO);
				entryNode.put("lat", point.getY());
				entryNode.put("lon", point.getX());
				entryNode.put("count", bucket.docCount());
				sumValue.ifPresent(v -> addValue(entryNode, "sum", v));
			}
		}
		return result;
	}

	private void addValue(ObjectNode parent, String property, double value) {
		if (!Units.isDimensionless(unit)) {
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
					Objects.requireNonNull(options.get("id")),
					Objects.requireNonNull(options.get("field", String.class, Event.LOCATION.getName())),
					options.get("value_field", String.class, null),
					unit != null ? Units.valueOf(unit) : Unit.ONE,
					Objects.requireNonNull(options.get("precision", Integer.class, 8)),
					filterParser.parse(options.get("filter")));
		};
	}
}
