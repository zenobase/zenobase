package com.zenobase.search.facets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.zenobase.json.Nodes;
import com.zenobase.search.constraints.FilterParser;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

public class CountFacet extends FilteredFacet {

	public static final String TYPE = "count";
	public static final String LABEL_MORE = "...";

	private final String field;
	private final String order;
	private final int offset;
	private final int limit;

	private CountFacet(String id, String field, String order, int offset, int limit, @Nullable Query filter) {
		super(id, filter);
		this.field = field;
		this.order = order;
		this.offset = offset;
		this.limit = limit;
	}

	@Override
	public void configure(SearchRequest.Builder builder) {
		Aggregation terms = Aggregation.of(a ->
			a.terms(t -> {
				t.field(field).size(offset + limit);
				boolean asc = !order.startsWith("-");
				String orderField = asc ? order : order.substring(1);
				SortOrder sortOrder = asc ? SortOrder.Asc : SortOrder.Desc;
				switch (orderField) {
					case "count" -> t.order(Collections.singletonMap("_count", sortOrder));
					case "term" -> t.order(Collections.singletonMap("_key", sortOrder));
				}
				return t;
			})
		);
		addAggregation(getId(), terms, builder);
	}

	@Override
	public JsonNode process(SearchResponse<ObjectNode> response) {
		ArrayNode result = Nodes.newArray();
		Aggregate aggregate = getAggregate(response);
		if (aggregate == null) {
			return result;
		}
		Preconditions.checkArgument(aggregate.isSterms(), "Can't count by non-keyword field: %s", field);
		List<StringTermsBucket> entries = aggregate.sterms().buckets().array();
		if (offset < entries.size()) {
			for (StringTermsBucket entry : entries.subList(offset, Math.min(entries.size(), offset + limit))) {
				ObjectNode entryNode = result.addObject();
				entryNode.put("label", entry.key());
				entryNode.put("count", entry.docCount());
			}
			Long sumOther = aggregate.sterms().sumOtherDocCount();
			if (sumOther != null && sumOther > 0) {
				ObjectNode entryNode = result.addObject();
				entryNode.put("label", LABEL_MORE);
				entryNode.put("count", sumOther);
			}
		}
		return result;
	}

	public static FacetBuilder builder(FilterParser filterParser) {
		return options ->
			new CountFacet(
				Objects.requireNonNull(options.get("id")),
				Objects.requireNonNull(options.get("field")),
				Objects.requireNonNull(options.get("order", String.class, "-count")),
				Objects.requireNonNull(options.get("offset", Integer.class, 0)),
				Objects.requireNonNull(options.get("limit", Integer.class, 10)),
				filterParser.parse(options.get("filter"))
			);
	}
}
