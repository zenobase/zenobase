package com.zenobase.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;

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
	private final QueryBuilder filter;

	public ListFacet(String id, int offset, int limit, String order, QueryBuilder filter, Schema schema) {
		super(id);
		this.offset = offset;
		this.limit = limit;
		this.filter = filter;
		this.order = SearchOrder.valueOf(order, schema);
	}

	@Override
	public void configure(SearchSourceBuilder builder) {
		builder.from(offset);
		builder.size(limit);
		builder.postFilter(filter);
		order.apply(builder);
		builder.version(Boolean.TRUE);
		builder.seqNoAndPrimaryTerm(Boolean.TRUE);
	}

	@Override
	public JsonNode process(SearchResponse response) {
		ArrayNode eventsNode = Nodes.newArray();
		for (SearchHit hit : response.getHits()) {
			Event event = new Event(Nodes.readObject(hit.getSourceRef().toBytesRef().bytes));
			event.setVersion(hit.getVersion());
			DomainNode.SEQ_NO.setValue(event.toJson(), hit.getSeqNo());
			DomainNode.PRIMARY_TERM.setValue(event.toJson(), hit.getPrimaryTerm());
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
