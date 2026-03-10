package com.zenobase.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.common.geo.GeoPoint;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.geogrid.GeoGrid;
import org.opensearch.search.builder.SearchSourceBuilder;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.search.geo.GeoBoundingBox;
import com.zenobase.search.geo.GeoCluster;
import com.zenobase.search.geo.GeoClusterBuilder;

public class MapFacet extends FilteredFacet {

	public static final String TYPE = "map";

	private final String field;
	private final int precision;

	private MapFacet(String id, String field, double factor, QueryBuilder filter) {
		super(id, filter);
		Preconditions.checkArgument(factor >= 0.0 && factor <= 1.0, "invalid factor value: %d", factor);
		this.field = field;
		this.precision = 9 - Ints.checkedCast(Math.round(factor * 5)); // [1.0..0.0] -> [4..9];
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.aggregation(AggregationBuilders.geohashGrid(getId()).field(field).precision(precision));
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode result = Nodes.newArray();
		GeoGrid grid = getAggregation(response);
		GeoClusterBuilder builder = new GeoClusterBuilder();
		for (GeoGrid.Bucket bucket : grid.getBuckets()) {
			builder.add(bucket.getDocCount(), bucket.getKeyAsString(), GeoPoint.fromGeohash(bucket.getKeyAsString()));
		}
		for (GeoCluster cluster : builder.build()) {
			GeoBoundingBox bounds = cluster.bounds();
			ObjectNode entryNode = result.addObject();
			entryNode.put("count", cluster.count());
			entryNode.put("lat", cluster.center().lat());
			entryNode.put("lon", cluster.center().lon());
			entryNode.put("lat_min", bounds.bottomRight().getLat());
			entryNode.put("lat_max", bounds.topLeft().getLat());
			entryNode.put("lon_min", bounds.topLeft().getLon());
			entryNode.put("lon_max", bounds.bottomRight().getLon());
		}
		return result;
	}

	public static FacetBuilder builder(FilterParser filterParser) {
		return options -> new MapFacet(
			options.get("id"),
			options.get("field", String.class, Event.LOCATION.getName()),
			options.get("factor", Double.class, 0.2),
			filterParser.parse(options.get("filter")));
	}
}
