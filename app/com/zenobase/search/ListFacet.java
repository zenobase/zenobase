package com.zenobase.search;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import com.zenobase.json.Nodes;
import com.zenobase.json.Schema;
import com.zenobase.models.Event;
import com.zenobase.services.SearchOrder;

public class ListFacet extends Facet {

	public static final String TYPE = "list";

	private final int offset;
	private final int limit;
	private final SearchOrder order;

	public ListFacet(String id, int offset, int limit, String order, Schema schema) {
		super(id);
		this.offset = offset;
		this.limit = limit;
		this.order = SearchOrder.valueOf(order, schema);
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.from(offset);
		builder.size(limit);
		order.apply(builder);
		builder.version(Boolean.TRUE);
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode eventsNode = Nodes.newArray();
		for (SearchHit hit : response.getHits()) {
			Event event = new Event(Nodes.readObject(hit.source()));
			event.setVersion(hit.version());
			eventsNode.add(event.toJson());
		}
		return eventsNode;
	}

	public static FacetBuilder builder(final Schema schema) {
		return new FacetBuilder() {
			@Override
			public Facet build(FacetOptions options) {
				return new ListFacet(
					options.get("id"),
					options.get("offset", Integer.class, 0),
					options.get("limit", Integer.class, 10),
					options.get("order", String.class, "-timestamp"),
					schema);
			}
		};
	}
}
