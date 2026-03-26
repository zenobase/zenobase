package com.zenobase.search;

import java.util.Objects;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import org.jspecify.annotations.Nullable;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.io.GeohashUtils;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.GeoHashGridBucket;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.search.geo.GeoBoundingBox;
import com.zenobase.search.geo.GeoCluster;
import com.zenobase.search.geo.GeoClusterBuilder;

public class MapFacet extends FilteredFacet {

	public static final String TYPE = "map";

	private final String field;
	private final int precision;

	private MapFacet(String id, String field, double factor, @Nullable Query filter) {
		super(id, filter);
		Preconditions.checkArgument(factor >= 0.0 && factor <= 1.0, "invalid factor value: %s", factor);
		this.field = field;
		this.precision = 9 - Ints.checkedCast(Math.round(factor * 5)); // [1.0..0.0] -> [4..9];
	}

	@Override
	public void configure(SearchRequest.Builder builder) {
		Aggregation grid =
				Aggregation.of(a -> a.geohashGrid(g -> g.field(field).precision(p -> p.geohashLength(precision))));
		addAggregation(getId(), grid, builder);
	}

	@Override
	public JsonNode process(SearchResponse<ObjectNode> response) {
		ArrayNode result = Nodes.newArray();
		Aggregate agg = Objects.requireNonNull(getAggregate(response));
		var builder = new GeoClusterBuilder();
		for (GeoHashGridBucket bucket : agg.geohashGrid().buckets().array()) {
			builder.add(bucket.docCount(), bucket.key(), GeohashUtils.decode(bucket.key(), SpatialContext.GEO));
		}
		for (GeoCluster cluster : builder.build()) {
			GeoBoundingBox bounds = cluster.bounds();
			ObjectNode entryNode = result.addObject();
			entryNode.put("count", cluster.count());
			entryNode.put("lat", cluster.center().getY());
			entryNode.put("lon", cluster.center().getX());
			entryNode.put("lat_min", bounds.bottomRight().getY());
			entryNode.put("lat_max", bounds.topLeft().getY());
			entryNode.put("lon_min", bounds.topLeft().getX());
			entryNode.put("lon_max", bounds.bottomRight().getX());
		}
		return result;
	}

	public static FacetBuilder builder(FilterParser filterParser) {
		return options -> new MapFacet(
				Objects.requireNonNull(options.get("id")),
				Objects.requireNonNull(options.get("field", String.class, Event.LOCATION.getName())),
				Objects.requireNonNull(options.get("factor", Double.class, 0.2)),
				filterParser.parse(options.get("filter")));
	}
}
