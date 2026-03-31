package com.zenobase.search;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

public abstract class FilteredFacet extends Facet {

	private final @Nullable Query filter;

	protected FilteredFacet(String id, @Nullable Query filter) {
		super(id);
		this.filter = filter;
	}

	protected void addAggregation(String name, Aggregation aggregation, SearchRequest.Builder builder) {
		if (filter != null) {
			Aggregation filtered = Aggregation.of(a -> a.filter(filter).aggregations(name, aggregation));
			builder.aggregations(getId(), filtered);
		} else {
			builder.aggregations(name, aggregation);
		}
	}

	protected @Nullable Aggregate getAggregate(SearchResponse<ObjectNode> response) {
		var aggregations = response.aggregations();
		if (aggregations == null) {
			return null;
		}
		var aggregate = aggregations.get(getId());
		if (aggregate != null && aggregate.isFilter()) {
			return aggregate.filter().aggregations().get(getId());
		}
		return aggregate;
	}
}
