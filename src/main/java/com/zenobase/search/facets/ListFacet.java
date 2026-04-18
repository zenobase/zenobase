package com.zenobase.search.facets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.Nodes;
import com.zenobase.json.OptimisticLock;
import com.zenobase.json.Schema;
import com.zenobase.models.Event;
import com.zenobase.search.constraints.FilterParser;
import com.zenobase.services.SearchOrder;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;

public class ListFacet extends Facet {

	public static final String TYPE = "list";

	private final int offset;
	private final int limit;
	private final SearchOrder order;
	private final @Nullable Query filter;

	public ListFacet(String id, int offset, int limit, String order, @Nullable Query filter, Schema schema) {
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
			Event event = new Event(Objects.requireNonNull(hit.source()));
			if (hit.version() != null) {
				event.setVersion(hit.version());
			}
			event.setOptimisticLock(
				new OptimisticLock(Objects.requireNonNull(hit.seqNo()), Objects.requireNonNull(hit.primaryTerm()))
			);
			eventsNode.add(event.toJson());
		}
		return eventsNode;
	}

	public static FacetBuilder builder(FilterParser filterParser, Schema schema) {
		return options ->
			new ListFacet(
				Objects.requireNonNull(options.get("id")),
				Objects.requireNonNull(options.get("offset", Integer.class, 0)),
				Objects.requireNonNull(options.get("limit", Integer.class, 10)),
				Objects.requireNonNull(options.get("order", String.class, "-timestamp")),
				filterParser.parse(options.get("filter")),
				schema
			);
	}
}
