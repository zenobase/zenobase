package com.zenobase.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;

import com.zenobase.json.DomainNode;
import com.zenobase.json.Nodes;
import com.zenobase.json.Schema;
import com.zenobase.models.Event;
import com.zenobase.services.SearchOrder;

public class ListFacet extends Facet {

	public static final String TYPE = "list";

	private final int offset;
	private final int limit;
	private final SearchOrder order;
	private final Query filter;

	public ListFacet(String id, int offset, int limit, String order, Query filter, Schema schema) {
		super(id);
		this.offset = offset;
		this.limit = limit;
		this.filter = filter;
		this.order = SearchOrder.valueOf(order, schema);
	}

	@Override
	public void configure(SearchRequest.Builder builder) {
		builder.from(offset);
		builder.size(limit);
		if (filter != null) {
			builder.postFilter(filter);
		}
		order.apply(builder);
		builder.version(true);
		builder.seqNoPrimaryTerm(true);
	}

	@Override
	public JsonNode process(SearchResponse<ObjectNode> response) {
		ArrayNode eventsNode = Nodes.newArray();
		for (Hit<ObjectNode> hit : response.hits().hits()) {
			Event event = new Event(hit.source());
			if (hit.version() != null) {
				event.setVersion(hit.version());
			}
			DomainNode.SEQ_NO.setValue(event.toJson(), hit.seqNo());
			DomainNode.PRIMARY_TERM.setValue(event.toJson(), hit.primaryTerm());
			eventsNode.add(event.toJson());
		}
		return eventsNode;
	}

	public static FacetBuilder builder(FilterParser filterParser, Schema schema) {
		return options -> new ListFacet(
			options.get("id"),
			options.get("offset", Integer.class, 0),
			options.get("limit", Integer.class, 10),
			options.get("order", String.class, "-timestamp"),
			filterParser.parse(options.get("filter")),
			schema);
	}
}
