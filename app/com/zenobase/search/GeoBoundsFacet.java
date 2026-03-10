package com.zenobase.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.common.geo.GeoPoint;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.metrics.GeoBounds;
import org.opensearch.search.builder.SearchSourceBuilder;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;

public class GeoBoundsFacet extends FilteredFacet {

	public static final String TYPE = "geobounds";

	private final String field;

	private GeoBoundsFacet(String id, String field, QueryBuilder filter) {
		super(id, filter);
		this.field = field;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		addAggregation(AggregationBuilders.geoBounds(getId()).field(field), builder);
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ObjectNode result = Nodes.newObject();
		GeoBounds bounds = getAggregation(response);
		GeoPoint sw = bounds.bottomRight();
		GeoPoint ne = bounds.topLeft();
		if (sw != null && ne != null) {
			result.put("lat_min", sw.getLat());
			result.put("lat_max", ne.getLat());
			result.put("lon_min", ne.getLon());
			result.put("lon_max", sw.getLon());
		}
		return result;
	}

	public static FacetBuilder builder(FilterParser filterParser) {
		return options -> new GeoBoundsFacet(
			options.get("id"),
			options.get("field", String.class, Event.LOCATION.getName()),
			filterParser.parse(options.get("filter")));
	}
}
