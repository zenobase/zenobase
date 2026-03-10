package com.zenobase.search;

import org.opensearch.action.search.SearchResponse;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.aggregations.Aggregation;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.HasAggregations;
import org.opensearch.search.builder.SearchSourceBuilder;

public abstract class FilteredFacet extends Facet {

	private final QueryBuilder filter;

	protected FilteredFacet(String id, QueryBuilder filter) {
		super(id);
		this.filter = filter;
	}

	protected void addAggregation(AggregationBuilder aggregation, SearchSourceBuilder builder) {
		if (filter != null) {
			aggregation = AggregationBuilders.filter(getId(), filter).subAggregation(aggregation);
		}
		builder.aggregation(aggregation);
	}

	protected <A extends Aggregation> A getAggregation(SearchResponse response) {
		A aggregation = response.getAggregations().get(getId());
		if (aggregation instanceof HasAggregations) {
			aggregation = ((HasAggregations) aggregation).getAggregations().get(getId());
		}
		return aggregation;
	}
}
