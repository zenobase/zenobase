package com.zenobase.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.zenobase.json.Nodes;
import com.zenobase.models.Event;

public class ListFacet extends Facet {

	public static final String TYPE = "list";

	private final int offset;
	private final int limit;
	private final String sort;
	private final SortOrder order;

	public ListFacet(String id, int offset, int limit, String sort, SortOrder order) {
		super(id);
		this.offset = offset;
		this.limit = limit;
		this.sort = sort;
		this.order = order;
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.from(offset);
		builder.size(limit);
		builder.sort(sort, order);
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

	public static FacetBuilder builder() {
		return new FacetBuilder() {
			@Override
			public Facet build(FacetOptions options) {
				return new ListFacet(
					options.get("id"),
					options.get("offset", Integer.class, 0),
					options.get("limit", Integer.class, 10),
					options.get("order", String.class, Event.TIMESTAMP.getName()),
					options.get("reverse", Boolean.class, Boolean.FALSE) ? SortOrder.ASC : SortOrder.DESC);
			}
		};
	}
}
