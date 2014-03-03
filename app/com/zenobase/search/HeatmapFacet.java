package com.zenobase.search;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoHashGrid;
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoHashGrid.Bucket;
import org.elasticsearch.search.aggregations.bucket.geogrid.GeoHashGridBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;

public class HeatmapFacet extends Facet {

	public static final String TYPE = "heatmap";

	private final String field;
	private final int precision;

	private HeatmapFacet(String id, String field, int precision) {
		super(id);
		this.field = field;
		this.precision = precision;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.aggregation(new GeoHashGridBuilder(getId()).field(field).precision(precision));
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode result = Nodes.newArray();
		GeoHashGrid grid = response.getAggregations().get(getId());
		for (Bucket bucket : grid.getBuckets()) {
			ObjectNode entryNode = result.addObject();
			entryNode.put("count", bucket.getDocCount());
			GeoPoint point = bucket.getKeyAsGeoPoint();
			entryNode.put("lat", point.lat());
			entryNode.put("lon", point.lon());
		}
		return result;
	}

	public static FacetBuilder builder() {
		return new FacetBuilder() {
			@Override
			public Facet build(FacetOptions options) {
				return new HeatmapFacet(
					options.get("id"),
					options.get("field", String.class, Event.LOCATION.getName()),
					options.get("precision", Integer.class, 8));
			}
		};
	}
}
