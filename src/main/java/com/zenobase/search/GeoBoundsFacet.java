package com.zenobase.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensearch.client.opensearch._types.GeoLocation;
import org.opensearch.client.opensearch._types.TopLeftBottomRightGeoBounds;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.GeoBoundsAggregate;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;

public class GeoBoundsFacet extends FilteredFacet {

	public static final String TYPE = "geobounds";

	private final String field;

	private GeoBoundsFacet(String id, String field, Query filter) {
		super(id, filter);
		this.field = field;
	}

	@Override
	public void configure(SearchRequest.Builder builder) {
		addAggregation(getId(), Aggregation.of(a -> a.geoBounds(g -> g.field(field))), builder);
	}

	@Override
	public JsonNode process(SearchResponse<ObjectNode> response) {
		ObjectNode result = Nodes.newObject();
		GeoBoundsAggregate bounds = getAggregate(response).geoBounds();
		if (bounds.bounds() != null
				&& bounds.bounds()._kind() == org.opensearch.client.opensearch._types.GeoBounds.Kind.Tlbr) {
			TopLeftBottomRightGeoBounds tlbr = bounds.bounds().tlbr();
			GeoLocation tl = tlbr.topLeft();
			GeoLocation br = tlbr.bottomRight();
			if (tl.isLatlon() && br.isLatlon()) {
				result.put("lat_min", br.latlon().lat());
				result.put("lat_max", tl.latlon().lat());
				result.put("lon_min", tl.latlon().lon());
				result.put("lon_max", br.latlon().lon());
			}
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
