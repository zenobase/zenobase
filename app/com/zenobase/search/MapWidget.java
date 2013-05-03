package com.zenobase.search;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;
import com.zenobase.search.facet.geocluster.GeoCluster;
import com.zenobase.search.facet.geocluster.GeoClusterFacet;
import com.zenobase.search.facet.geocluster.GeoClusterFacetBuilder;

public class MapWidget extends Widget {

	public static final String TYPE = "map";

	private final String field;
	private final double factor;

	private MapWidget(String id, String field, double factor) {
		super(id);
		this.field = field;
		this.factor = factor;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.facet(new GeoClusterFacetBuilder(getId(), field, factor));
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode result = Nodes.newArray();
		GeoClusterFacet facet = response.getFacets().facet(GeoClusterFacet.class, getId());
		for (GeoCluster entry : facet.getEntries()) {
			ObjectNode entryNode = result.addObject();
			entryNode.put("count", entry.size());
			entryNode.put("lat", entry.center().lat());
			entryNode.put("lon", entry.center().lon());
			if (entry.size() > 1) {
				entryNode.put("lat_min", entry.bounds().bottomRight().getLat());
				entryNode.put("lat_max", entry.bounds().topLeft().getLat());
				entryNode.put("lon_min", entry.bounds().topLeft().getLon());
				entryNode.put("lon_max", entry.bounds().bottomRight().getLon());
			}
		}
		return result;
	}

	public static WidgetBuilder builder() {
		return new WidgetBuilder() {
			@Override
			public Widget build(WidgetOptions options) {
				return new MapWidget(
					options.get("id"),
					options.get("field", String.class, Event.LOCATION.getName()),
					options.get("factor", Double.class, 0.2));
			}
		};
	}
}
