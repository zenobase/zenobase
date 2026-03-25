package com.zenobase.search;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

import com.zenobase.json.Nodes;

public class CountFacet extends FilteredFacet {

	public static final String TYPE = "count";
	public static final String LABEL_MORE = "...";

	private final String field;
	private final String order;
	private final int offset;
	private final int limit;

	private CountFacet(String id, String field, String order, int offset, int limit, Query filter) {
		super(id, filter);
		this.field = field;
		this.order = order;
		this.offset = offset;
		this.limit = limit;
	}

	@Override
	public void configure(SearchRequest.Builder builder) {
		Aggregation terms = Aggregation.of(a -> a.terms(t -> {
			t.field(field).size(offset + limit);
			boolean asc = !order.startsWith("-");
			String orderField = asc ? order : order.substring(1);
			SortOrder sortOrder = asc ? SortOrder.Asc : SortOrder.Desc;
			switch (orderField) {
				case "count" -> t.order(Collections.singletonMap("_count", sortOrder));
				case "term" -> t.order(Collections.singletonMap("_key", sortOrder));
			}
			return t;
		}));
		addAggregation(getId(), terms, builder);
	}

	@Override
	public JsonNode process(SearchResponse<ObjectNode> response) {
		ArrayNode result = Nodes.newArray();
		Aggregate agg = getAggregate(response);
		List<StringTermsBucket> entries = agg.sterms().buckets().array();
		if (offset < entries.size()) {
			for (StringTermsBucket entry : entries.subList(offset, Math.min(entries.size(), offset + limit))) {
				ObjectNode entryNode = result.addObject();
				entryNode.put("label", entry.key());
				entryNode.put("count", entry.docCount());
			}
			long sumOther = agg.sterms().sumOtherDocCount();
			if (sumOther > 0) {
				ObjectNode entryNode = result.addObject();
				entryNode.put("label", LABEL_MORE);
				entryNode.put("count", sumOther);
			}
		}
		return result;
	}

	public static FacetBuilder builder(FilterParser filterParser) {
		return options -> new CountFacet(
			options.get("id"),
			options.get("field"),
			options.get("order", String.class, "-count"),
			options.get("offset", Integer.class, 0),
			options.get("limit", Integer.class, 10),
			filterParser.parse(options.get("filter")));
	}
}
