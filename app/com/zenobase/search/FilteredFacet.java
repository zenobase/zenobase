package com.zenobase.search;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.HasAggregations;
import org.elasticsearch.search.builder.SearchSourceBuilder;

public abstract class FilteredFacet extends Facet {

	private final FilterBuilder filter;

	protected FilteredFacet(String id, FilterBuilder filter) {
		super(id);
		this.filter = filter;
	}

	protected void addAggregation(AbstractAggregationBuilder aggregation, SearchSourceBuilder builder) {
		if (filter != null) {
			aggregation = AggregationBuilders.filter(getId()).filter(filter).subAggregation(aggregation);
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
