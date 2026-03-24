package com.zenobase.search;

import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

public abstract class FilteredFacet extends Facet {

	private final Query filter;

	protected FilteredFacet(String id, Query filter) {
		super(id);
		this.filter = filter;
	}

	protected void addAggregation(String name, Aggregation aggregation, SearchRequest.Builder builder) {
		if (filter != null) {
			Aggregation filtered = Aggregation.of(a -> a
				.filter(filter)
				.aggregations(name, aggregation)
			);
			builder.aggregations(getId(), filtered);
		} else {
			builder.aggregations(name, aggregation);
		}
	}

	@SuppressWarnings("unchecked")
	protected Aggregate getAggregate(SearchResponse<ObjectNode> response) {
		Aggregate agg = response.aggregations().get(getId());
		if (agg != null && agg.isFilter()) {
			return agg.filter().aggregations().get(getId());
		}
		return agg;
	}
}
